package com.jetbrains.edu.learning

import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.impl.ProjectJdkImpl
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.util.SystemInfo
import com.intellij.ui.ComboboxWithBrowseButton
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.PyDirectoryProjectGenerator.getBaseSdk
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.python.newProject.PyNewProjectSettings
import com.jetbrains.python.sdk.flavors.PythonSdkFlavor
import icons.PythonIcons
import java.awt.BorderLayout
import javax.swing.JComponent

internal abstract class PyLanguageSettings : EduPluginConfigurator.LanguageSettings<PyNewProjectSettings> {

  protected val mySettings: PyNewProjectSettings = PyNewProjectSettings()

  override fun getLanguageSettingsComponent(course: Course): LabeledComponent<JComponent> {
    // by default we create new virtual env in project, we need to add this non-existing sdk to sdk list
    val fakeSdk = createFakeSdk(course)

    val combo = getInterpreterComboBox(fakeSdk)
    if (SystemInfo.isMac && !UIUtil.isUnderDarcula()) {
      combo.putClientProperty("JButton.buttonType", null)
    }
    combo.setButtonIcon(PythonIcons.Python.InterpreterGear)
    return LabeledComponent.create(combo, "Interpreter", BorderLayout.WEST)
  }

  override fun getSettings(): PyNewProjectSettings = mySettings

  protected abstract fun getInterpreterComboBox(fakeSdk: Sdk?): ComboboxWithBrowseButton

  companion object {

    private fun createFakeSdk(course: Course): ProjectJdkImpl? {
      val fakeSdkPath = getBaseSdk(course) ?: return null
      val flavor = PythonSdkFlavor.getApplicableFlavors(false)[0]
      val prefix = flavor.name + " "
      val versionString = flavor.getVersionString(fakeSdkPath)
      if (versionString == null || !versionString.contains(prefix)) {
        return null
      }
      val name = "new virtual env " + versionString.substring(prefix.length)
      return ProjectJdkImpl(name, FakePythonSdkType)
    }
  }
}
