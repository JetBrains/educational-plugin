package com.jetbrains.edu.jvm

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil
import com.intellij.openapi.project.ProjectManager
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
import com.intellij.util.concurrency.annotations.RequiresEdt
import com.jetbrains.edu.jvm.messages.EduJVMBundle
import com.jetbrains.edu.learning.EduNames.ENVIRONMENT_CONFIGURATION_LINK_JAVA
import com.jetbrains.edu.learning.LanguageSettings
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.ui.errors.SettingsValidationResult
import com.jetbrains.edu.learning.newproject.ui.errors.ValidationMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.BorderLayout
import java.io.File
import javax.swing.JComponent

open class JdkLanguageSettings : LanguageSettings<JdkProjectSettings>() {

  private var jdkSelection: JdkSelection = NOTHING_SELECTED
  protected val sdkModel: ProjectSdksModel = createSdkModel()
  protected val jdk: Sdk? get() = jdkSelection.jdk

  private fun createSdkModel(): ProjectSdksModel {
    val project = ProjectManager.getInstance().defaultProject
    return ProjectStructureConfigurable.getInstance(project).projectJdksModel.apply {
      reset(project)
      setupProjectSdksModel(this)
    }
  }

  protected open fun setupProjectSdksModel(model: ProjectSdksModel) {}

  fun selectJdk(jdk: Sdk?, setByUser: Boolean = false) {
    logErrorOnNonEdtCall()
    if (!setByUser && jdkSelection.setByUser) return
    jdkSelection = JdkSelection(jdk, setByUser)
  }

  override fun getLanguageSettingsComponents(
    course: Course,
    disposable: CheckedDisposable,
    context: UserDataHolder?
  ): List<LabeledComponent<JComponent>> {
    val sdkTypeFilter = Condition<SdkTypeId> { sdkTypeId -> sdkTypeId is JavaSdkType && !(sdkTypeId as JavaSdkType).isDependent }
    val sdkFilter = Condition<Sdk> { sdk -> ExternalSystemJdkUtil.isValidJdk(sdk) }
    val jdkComboBox = JdkComboBox(null, sdkModel, sdkTypeFilter, sdkFilter, sdkTypeFilter, null)

    jdkComboBox.addItemListener {
      selectJdk(jdkComboBox.selectedItem?.jdk, setByUser = true)
      notifyListeners()
    }

    waitForModality(jdkComboBox, disposable) { modalityState ->
      preselectJdk(course, jdkComboBox, sdkModel, modalityState, disposable)
    }

    selectJdk(jdkComboBox.selectedItem?.jdk, setByUser = false)

    return listOf(LabeledComponent.create(jdkComboBox, "JDK", BorderLayout.WEST))
  }

  @RequiresEdt
  private fun preselectJdk(
    course: Course,
    jdkComboBox: JdkComboBox,
    sdksModel: ProjectSdksModel,
    modalityState: ModalityState,
    disposable: Disposable
  ) {
    if (jdkComboBox.selectedJdk != null) return

    val courseSdkVersion = minJvmSdkVersion(course)
    EduJdkLookupService.getInstance().findSuitableJdk(courseSdkVersion, sdksModel, modalityState, disposable) { suitableJdk ->
      withContext(Dispatchers.EDT) {
        if (suitableJdk == null) {
            LOG.warn("Failed to find suitable JDK for course sdk version=$courseSdkVersion")
        }

        // Check whether JDK has not already been selected neither by a user nor automatically
        if (jdkSelection == NOTHING_SELECTED) {
          jdkComboBox.selectedJdk = suitableJdk
          // although there is a listener in the combobox, we should specify that JDK was selected not by a user
          jdkSelection = JdkSelection(suitableJdk, setByUser = false)
        }
      }
    }
  }

  override fun validate(course: Course?, courseLocation: String?): SettingsValidationResult {
    logErrorOnNonEdtCall()

    fun ready(messageId: String, vararg additionalSubstitution: String): SettingsValidationResult {
      val message = EduJVMBundle.message(messageId, *additionalSubstitution)

      return SettingsValidationResult.Ready(ValidationMessage(message, ENVIRONMENT_CONFIGURATION_LINK_JAVA))
    }

    course ?: return super.validate(null, courseLocation)

    if (jdkSelection == NOTHING_SELECTED) return SettingsValidationResult.Pending

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

  /**
   * Similar to the @RequiresEdt annotation, but only logs the error and does not prevent the method from running
   *
   * TODO make sure selectJdk() and validate() are always called from EDT. Change this call to the `@RequiresEdt` annotation.
   */
  private fun logErrorOnNonEdtCall() {
    if (!ApplicationManager.getApplication().isDispatchThread) {
      LOG.error("The method must be called from EDT", Throwable())
    }
  }

  companion object {
    private val LOG = logger<JdkLanguageSettings>()
    private val NOTHING_SELECTED = JdkSelection(null, setByUser = false)

    fun findBundledJdk(model: ProjectSdksModel): BundledJdkInfo? {
      val bundledJdkPath = PathManager.getBundledRuntimePath()
      // It's possible IDE doesn't have bundled jdk.
      // For example, IDE loaded by gradle-intellij-plugin doesn't have bundled jdk
      if (!File(bundledJdkPath).exists()) return null
      // Try to find existing bundled jdk added by the plugin on previous course creation or by user
      val sdk = model.projectSdks.values.find { it.homePath == bundledJdkPath }
      return BundledJdkInfo(bundledJdkPath, sdk)
    }
  }

  data class BundledJdkInfo(val path: String, val existingSdk: Sdk?)
  private data class JdkSelection(val jdk: Sdk?, val setByUser: Boolean)
}
