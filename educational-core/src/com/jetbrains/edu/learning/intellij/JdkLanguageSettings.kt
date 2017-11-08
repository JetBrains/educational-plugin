package com.jetbrains.edu.learning.intellij

import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.projectRoots.JavaSdkType
import com.intellij.openapi.projectRoots.SdkTypeId
import com.intellij.openapi.roots.ui.configuration.JdkComboBox
import com.intellij.openapi.roots.ui.configuration.ProjectStructureConfigurable
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.util.Condition
import com.intellij.openapi.util.Conditions
import com.intellij.ui.ComboboxWithBrowseButton
import com.jetbrains.edu.learning.EduPluginConfigurator
import com.jetbrains.edu.learning.courseFormat.Course
import java.awt.BorderLayout
import javax.swing.JComponent

internal class JdkLanguageSettings : EduPluginConfigurator.LanguageSettings<JdkProjectSettings> {

  private val myModel: ProjectSdksModel = ProjectStructureConfigurable.getInstance(ProjectManager.getInstance().defaultProject).projectJdksModel
  private var myJdkSettings: JdkProjectSettings = JdkProjectSettings(myModel, null)

  override fun getLanguageSettingsComponent(course: Course): LabeledComponent<JComponent> {
    val sdkTypeFilter = Condition<SdkTypeId> { sdkTypeId -> sdkTypeId is JavaSdkType && !(sdkTypeId as JavaSdkType).isDependent }
    val jdkComboBox = JdkComboBox(myModel, sdkTypeFilter, Conditions.alwaysTrue(), sdkTypeFilter, true)
    val comboboxWithBrowseButton = ComboboxWithBrowseButton(jdkComboBox)
    val setupButton = comboboxWithBrowseButton.button
    jdkComboBox.setSetupButton(setupButton, null, myModel, jdkComboBox.selectedItem, null, false)
    myJdkSettings = JdkProjectSettings(myModel, jdkComboBox.selectedItem)
    jdkComboBox.addItemListener {
      myJdkSettings = JdkProjectSettings(myModel, jdkComboBox.selectedItem)
    }
    return LabeledComponent.create(comboboxWithBrowseButton, "Jdk", BorderLayout.WEST)
  }

  override fun getSettings(): JdkProjectSettings = myJdkSettings
}
