package com.jetbrains.edu.python.learning.run

import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import com.jetbrains.edu.python.learning.messages.EduPythonBundle
import com.jetbrains.python.run.AbstractPyCommonOptionsForm
import com.jetbrains.python.run.AbstractPythonRunConfiguration
import com.jetbrains.python.run.PyCommonOptionsFormData
import com.jetbrains.python.run.PyCommonOptionsFormFactory
import javax.swing.JComponent
import javax.swing.JTextField

class PySettingsEditor(private val project: Project) : SettingsEditor<PyRunTestConfiguration>() {
  private lateinit var form: AbstractPyCommonOptionsForm
  private lateinit var pathToTestFileField: JTextField

  override fun resetEditorFrom(configuration: PyRunTestConfiguration) {
    AbstractPythonRunConfiguration.copyParams(configuration, form)
    pathToTestFileField.text = configuration.pathToTest
  }

  override fun applyEditorTo(configuration: PyRunTestConfiguration) {
    AbstractPythonRunConfiguration.copyParams(form, configuration)
    configuration.pathToTest = pathToTestFileField.text
  }

  override fun createEditor(): JComponent {
    form = createEnvPanel()
    return panel {
      row(EduPythonBundle.message("run.configuration.path.to.tests")) {
        pathToTestFileField = textField().align(AlignX.FILL).component
      }
      row {
        cell(form.mainPanel).align(AlignX.FILL)
      }
    }
  }

  private fun createEnvPanel(): AbstractPyCommonOptionsForm {
    return PyCommonOptionsFormFactory.getInstance().createForm(object : PyCommonOptionsFormData {
      override fun getProject() = this@PySettingsEditor.project

      override fun getValidModules() = AbstractPythonRunConfiguration.getValidModules(this@PySettingsEditor.project)

      override fun showConfigureInterpretersLink() = false
    })
  }
}
