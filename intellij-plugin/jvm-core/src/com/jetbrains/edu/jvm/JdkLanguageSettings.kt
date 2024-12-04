package com.jetbrains.edu.jvm

import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.JavaSdkType
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.SdkTypeId
import com.intellij.openapi.roots.ui.configuration.JdkComboBox
import com.intellij.openapi.roots.ui.configuration.ProjectStructureConfigurable
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.util.CheckedDisposable
import com.intellij.openapi.util.Condition
import com.intellij.openapi.util.UserDataHolder
import com.jetbrains.edu.jvm.messages.EduJVMBundle
import com.jetbrains.edu.learning.EduNames.ENVIRONMENT_CONFIGURATION_LINK_JAVA
import com.jetbrains.edu.learning.LanguageSettings
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.project
import com.jetbrains.edu.learning.newproject.ui.errors.SettingsValidationResult
import com.jetbrains.edu.learning.newproject.ui.errors.ValidationMessage
import com.jetbrains.edu.learning.runInBackground
import org.jetbrains.annotations.VisibleForTesting
import java.awt.BorderLayout
import java.io.File
import javax.swing.JComponent

open class JdkLanguageSettings : LanguageSettings<JdkProjectSettings>() {

  protected var jdk: Sdk? = null
  protected val sdkModel: ProjectSdksModel = createSdkModel()

  private fun createSdkModel(): ProjectSdksModel {
    val project = ProjectManager.getInstance().defaultProject
    return ProjectStructureConfigurable.getInstance(project).projectJdksModel.apply {
      reset(project)
      setupProjectSdksModel(this)
    }
  }

  protected open fun setupProjectSdksModel(model: ProjectSdksModel) {}

  @VisibleForTesting
  fun selectJdk(jdk: Sdk?) {
    this.jdk = jdk
  }

  override fun getLanguageSettingsComponents(
    course: Course,
    disposable: CheckedDisposable,
    context: UserDataHolder?
  ): List<LabeledComponent<JComponent>> {
    val sdkTypeFilter = Condition<SdkTypeId> { sdkTypeId -> sdkTypeId is JavaSdkType && !(sdkTypeId as JavaSdkType).isDependent }
    val sdkFilter = Condition<Sdk> { sdk -> sdkTypeFilter.value(sdk.sdkType) }
    val jdkComboBox = JdkComboBox(null, sdkModel, sdkTypeFilter, sdkFilter, sdkTypeFilter, null)
    preselectJdk(course, jdkComboBox, sdkModel)
    jdk = jdkComboBox.selectedItem?.jdk
    jdkComboBox.addItemListener {
      jdk = jdkComboBox.selectedItem?.jdk
      notifyListeners()
    }
    return listOf<LabeledComponent<JComponent>>(LabeledComponent.create(jdkComboBox, "JDK", BorderLayout.WEST))
  }

  private fun preselectJdk(course: Course, jdkComboBox: JdkComboBox, sdksModel: ProjectSdksModel) {
    if (jdkComboBox.selectedJdk != null) return
    runInBackground(course.project, EduJVMBundle.message("progress.setting.suitable.jdk"), false) {
      val suitableJdk = findSuitableJdk(minJvmSdkVersion(course), sdksModel)
      invokeLater(ModalityState.any()) {
        jdkComboBox.selectedJdk = suitableJdk
      }
    }
  }

  override fun validate(course: Course?, courseLocation: String?): SettingsValidationResult {
    fun ready(messageId: String, vararg additionalSubstitution: String): SettingsValidationResult {
      val message = EduJVMBundle.message(messageId, *additionalSubstitution)

      return SettingsValidationResult.Ready(ValidationMessage(message, ENVIRONMENT_CONFIGURATION_LINK_JAVA))
    }

    course ?: return super.validate(null, courseLocation)

    // compare the version of the selected jdk to the minimum version required by the course
    val selectedJavaVersion = ParsedJavaVersion.fromJavaSdkVersionString(jdk?.versionString)
    val courseJavaVersion = minJvmSdkVersion(course)

    if (courseJavaVersion is JavaVersionParseFailed) {
      return ready("error.unsupported.java.version", courseJavaVersion.versionAsText)
    }
    if (selectedJavaVersion is JavaVersionParseFailed) {
      return ready("failed.determine.java.version", selectedJavaVersion.versionAsText)
    }

    if (selectedJavaVersion == JavaVersionNotProvided) {
      return if (courseJavaVersion == JavaVersionNotProvided) {
        ready("error.no.jdk")
      }
      else {
        ready("error.no.jdk.need.at.least", (courseJavaVersion as JavaVersionParseSuccess).javaSdkVersion.description)
      }
    }

    selectedJavaVersion as JavaVersionParseSuccess

    val maxJavaVersion = maxJvmSdkVersion(course) as JavaVersionParseSuccess?

    if (maxJavaVersion != null && selectedJavaVersion isGreater maxJavaVersion) {
      return ready("error.new.java", maxJavaVersion.javaSdkVersion.description, selectedJavaVersion.javaSdkVersion.description)
    }

    if (courseJavaVersion == JavaVersionNotProvided) {
      return SettingsValidationResult.OK
    }

    courseJavaVersion as JavaVersionParseSuccess

    return if (selectedJavaVersion isAtLeast courseJavaVersion) {
      SettingsValidationResult.OK
    }
    else {
      ready("error.old.java", courseJavaVersion.javaSdkVersion.description, selectedJavaVersion.javaSdkVersion.description)
    }
  }

  /**
   * This is the minimum JDK version that we allow to use for the course.
   * Basically, it is taken from environment settings, but for Java courses it is specified explicitly in [Course.languageVersion]
   */
  protected open fun minJvmSdkVersion(course: Course): ParsedJavaVersion = course.minJvmSdkVersion

  /**
   * This is the maximum JDK version that we allow to use for the course.
   * For Hyperskill courses we don't allow using JDK greater than 19
   */
  protected open fun maxJvmSdkVersion(course: Course): ParsedJavaVersion? = course.maxJvmSdkVersion

  override fun getSettings(): JdkProjectSettings = JdkProjectSettings(sdkModel, jdk)

  companion object {
    fun findBundledJdk(model: ProjectSdksModel): BundledJdkInfo? {
      val bundledJdkPath = PathManager.getBundledRuntimePath()
      // It's possible IDE doesn't have bundled jdk.
      // For example, IDE loaded by gradle-intellij-plugin doesn't have bundled jdk
      if (!File(bundledJdkPath).exists()) return null
      // Try to find existing bundled jdk added by the plugin on previous course creation or by user
      val sdk = model.projectSdks.values.find { it.homePath == bundledJdkPath }
      return BundledJdkInfo(bundledJdkPath, sdk)
    }

    fun findSuitableJdk(courseSdkVersion: ParsedJavaVersion, sdkModel: ProjectSdksModel): Sdk? {
      val jdks = sdkModel.sdks.filter { it.sdkType == JavaSdk.getInstance() }

      if (courseSdkVersion !is JavaVersionParseSuccess) {
        return jdks.firstOrNull()
      }

      return jdks.find {
        val jdkVersion = ParsedJavaVersion.fromJavaSdkVersionString(it.versionString)
        if (jdkVersion is JavaVersionParseSuccess) {
          jdkVersion isAtLeast courseSdkVersion
        }
        else {
          false
        }
      }
    }
  }

  data class BundledJdkInfo(val path: String, val existingSdk: Sdk?)
}
