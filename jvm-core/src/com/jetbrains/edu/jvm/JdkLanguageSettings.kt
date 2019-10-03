package com.jetbrains.edu.jvm

import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.projectRoots.JavaSdkType
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.SdkTypeId
import com.intellij.openapi.roots.ui.configuration.JdkComboBox
import com.intellij.openapi.roots.ui.configuration.ProjectStructureConfigurable
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.util.Condition
import com.intellij.openapi.util.UserDataHolder
import com.intellij.ui.ComboboxWithBrowseButton
import com.jetbrains.edu.learning.LanguageSettings
import com.jetbrains.edu.learning.courseFormat.Course
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

  override fun getLanguageSettingsComponents(course: Course, context: UserDataHolder?): List<LabeledComponent<JComponent>> {
    val sdkTypeFilter = Condition<SdkTypeId> { sdkTypeId -> sdkTypeId is JavaSdkType && !(sdkTypeId as JavaSdkType).isDependent }
    val sdkFilter = Condition<Sdk> { sdk -> sdkTypeFilter.value(sdk.sdkType) }
    val jdkComboBox = JdkComboBox(myModel, sdkTypeFilter, sdkFilter, sdkTypeFilter, true)
    preselectJdk(course, jdkComboBox, myModel)
    val comboboxWithBrowseButton = ComboboxWithBrowseButton(jdkComboBox)
    val setupButton = comboboxWithBrowseButton.button
    jdkComboBox.setSetupButton(setupButton, null, myModel, jdkComboBox.selectedItem, null, false)
    myJdkSettings = JdkProjectSettings(myModel, jdkComboBox.selectedItem)
    jdkComboBox.addItemListener {
      myJdkSettings = JdkProjectSettings(myModel, jdkComboBox.selectedItem)
      notifyListeners()
    }
    return listOf<LabeledComponent<JComponent>>(LabeledComponent.create(comboboxWithBrowseButton, "JDK", BorderLayout.WEST))
  }

  protected open fun preselectJdk(course: Course, jdkComboBox: JdkComboBox, sdksModel: ProjectSdksModel) {

  }

  override fun getSettings(): JdkProjectSettings = myJdkSettings
}
