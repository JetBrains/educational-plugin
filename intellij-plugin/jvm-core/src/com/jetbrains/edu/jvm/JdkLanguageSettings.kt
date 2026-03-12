package com.jetbrains.edu.jvm

import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.runBlockingCancellable
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.JavaSdkType
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.SdkTypeId
import com.intellij.openapi.projectRoots.impl.jdkDownloader.JdkDownloadUtil
import com.intellij.openapi.roots.ui.configuration.JdkComboBox
import com.intellij.openapi.roots.ui.configuration.ProjectStructureConfigurable
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.util.CheckedDisposable
import com.intellij.openapi.util.Condition
import com.intellij.openapi.util.UserDataHolder
import com.intellij.openapi.application.writeAction
import com.jetbrains.edu.jvm.messages.EduJVMBundle
import com.jetbrains.edu.learning.EduNames.ENVIRONMENT_CONFIGURATION_LINK_JAVA
import com.jetbrains.edu.learning.LanguageSettings
import com.jetbrains.edu.learning.computeUnderProgress
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.project
import com.jetbrains.edu.learning.newproject.ui.errors.SettingsValidationResult
import com.jetbrains.edu.learning.newproject.ui.errors.ValidationMessage
import com.jetbrains.edu.learning.runInBackground
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
      if (suitableJdk == null) {
        LOG.warn("Failed to find suitable JDK for course ${course.name}")
      }
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

    if (courseJavaVersion == JavaVersionNotProvided) {
      return SettingsValidationResult.OK
    }

    selectedJavaVersion as JavaVersionParseSuccess
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

  override fun getSettings(): JdkProjectSettings = JdkProjectSettings(sdkModel, jdk)

  companion object {
    private val LOG = logger<JdkLanguageSettings>()

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

      val existingSdk = jdks.find {
        val jdkVersion = ParsedJavaVersion.fromJavaSdkVersionString(it.versionString)

        when {
          // if the course does not specify the version, all SDKs fit
          courseSdkVersion !is JavaVersionParseSuccess -> true
          // if the SDK doesn't specify the version, it does not fit
          jdkVersion !is JavaVersionParseSuccess -> false
          // otherwise, the version of the SDK must be at least the version required by the course
          else -> jdkVersion isAtLeast courseSdkVersion
        }
      }

      if (existingSdk != null) return existingSdk

      val requiredJDKFeature = (courseSdkVersion as? JavaVersionParseSuccess)?.javaSdkVersion?.maxLanguageLevel?.feature()

      return try {
        computeUnderProgress(project = null, "Installing tools", canBeCancelled = false) {
          runBlockingCancellable {
            createDownloadableSdk(requiredJDKFeature, sdkModel)
          }
        }
      }
      catch (th: Throwable) {
        LOG.error("Failed to create auto-downloadable JDK", th)
        null
      }
    }

    private suspend fun createDownloadableSdk(
      feature: Int?,
      sdkModel: ProjectSdksModel
    ): Sdk? {
      val project = ProjectManager.getInstance().defaultProject

      val (jdkItem, jdkHome) = JdkDownloadUtil.pickJdkItemAndPath(project) { item ->
        feature == null || item.jdkMajorVersion == feature
      } ?: return null

      val task = JdkDownloadUtil.createDownloadTask(project, jdkItem, jdkHome) ?: error("Failed to create download task")

      val sdk = try {
        withContext(Dispatchers.EDT) {
          writeAction {
            sdkModel.createIncompleteSdk(JavaSdk.getInstance(), task, null)
          }
        }
      }
      catch (th: Throwable) {
        LOG.error("Failed to create auto-downloadable JDK", th)
        throw th
      }

      return sdk
    }

    data class BundledJdkInfo(val path: String, val existingSdk: Sdk?)
  }
}
