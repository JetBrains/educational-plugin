package com.jetbrains.edu.learning

import com.intellij.openapi.diagnostic.Logger
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

  // Some python API has been changed while 2017.3 (first version of python plugin with new API is 2017.3.173.3415.6).
  // To prevent exceptions because of it we should check if it is new API or not.
  private val myHasOldPythonApi: Boolean = hasOldPythonApi()

  private fun hasOldPythonApi(): Boolean {
    return try {
      // `com.jetbrains.python.sdk.PySdkExtKt` is part of new python API
      // so we can use it to determine if it is new python API or not.
      // This way looks easier than check version because
      // there are different IDE with python support: PyCharm C/P/EDU and other IDEs with python plugin
      // and we have to use separate way to check API version for each case.
      Class.forName("com.jetbrains.python.sdk.PySdkExtKt")
      false
    } catch (e: ClassNotFoundException) {
      LOG.warn("Current python API is old")
      true
    }
  }

  override fun getLanguageSettingsComponent(course: Course): LabeledComponent<JComponent>? {
    // It is rather hard to support python interpreter combobox
    // and virtual env creation using old API
    // so we decided to turn off it in this case.
    if (myHasOldPythonApi) {
      LOG.warn("We won't show interpreter combobox because current python API is old")
      return null
    }

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

    private val LOG = Logger.getInstance(PyLanguageSettings::class.java)

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
