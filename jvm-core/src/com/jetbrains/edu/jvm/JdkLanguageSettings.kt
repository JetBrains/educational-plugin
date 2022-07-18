package com.jetbrains.edu.jvm

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.JavaSdkType
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.SdkTypeId
import com.intellij.openapi.roots.ui.configuration.JdkComboBox
import com.intellij.openapi.roots.ui.configuration.ProjectStructureConfigurable
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.util.Condition
import com.intellij.openapi.util.UserDataHolder
import com.jetbrains.edu.jvm.messages.EduJVMBundle
import com.jetbrains.edu.learning.EduNames.ENVIRONMENT_CONFIGURATION_LINK_JAVA
import com.jetbrains.edu.learning.LanguageSettings
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.ui.ValidationMessage
import java.awt.BorderLayout
import javax.swing.JComponent

open class JdkLanguageSettings : LanguageSettings<JdkProjectSettings>() {

  private val myModel: ProjectSdksModel = createSdkModel()
  protected var myJdkSettings: JdkProjectSettings = JdkProjectSettings(myModel, null)

  private fun createSdkModel(): ProjectSdksModel {
    val project = ProjectManager.getInstance().defaultProject
    return ProjectStructureConfigurable.getInstance(project).projectJdksModel.apply {
      reset(project)
      setupProjectSdksModel(this)
    }
  }

  protected open fun setupProjectSdksModel(model: ProjectSdksModel) {}

  override fun getLanguageSettingsComponents(course: Course, disposable: Disposable, context: UserDataHolder?): List<LabeledComponent<JComponent>> {
    val sdkTypeFilter = Condition<SdkTypeId> { sdkTypeId -> sdkTypeId is JavaSdkType && !(sdkTypeId as JavaSdkType).isDependent }
    val sdkFilter = Condition<Sdk> { sdk -> sdkTypeFilter.value(sdk.sdkType) }
    val jdkComboBox = JdkComboBox(null, myModel, sdkTypeFilter, sdkFilter, sdkTypeFilter, null)
    preselectJdk(course, jdkComboBox, myModel)
    myJdkSettings = JdkProjectSettings(myModel, jdkComboBox.selectedItem)
    jdkComboBox.addItemListener {
      myJdkSettings = JdkProjectSettings(myModel, jdkComboBox.selectedItem)
      notifyListeners()
    }
    return listOf<LabeledComponent<JComponent>>(LabeledComponent.create(jdkComboBox, "JDK", BorderLayout.WEST))
  }

  protected open fun preselectJdk(course: Course, jdkComboBox: JdkComboBox, sdksModel: ProjectSdksModel) {
    if (jdkComboBox.selectedJdk != null) return
    jdkComboBox.selectedJdk = sdksModel.sdks.firstOrNull { it.sdkType == JavaSdk.getInstance() }
  }

  override fun validate(course: Course?, courseLocation: String?): ValidationMessage? {
    if (myJdkSettings.jdkItem == null) {
      return ValidationMessage(EduJVMBundle.message("error.no.jdk"), ENVIRONMENT_CONFIGURATION_LINK_JAVA)
    }
    return super.validate(course, courseLocation)
  }

  override fun getSettings(): JdkProjectSettings = myJdkSettings
}
