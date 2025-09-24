package com.jetbrains.edu.sql.jvm.gradle

import com.intellij.openapi.Disposable
import com.intellij.openapi.observable.util.whenItemSelected
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.util.CheckedDisposable
import com.intellij.openapi.util.UserDataHolder
import com.jetbrains.edu.jvm.JdkLanguageSettings
import com.jetbrains.edu.jvm.JdkProjectSettings
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.sql.core.EduSqlBundle
import java.awt.BorderLayout
import java.awt.Component
import java.util.*
import javax.swing.*

class SqlJdkLanguageSettings : JdkLanguageSettings() {

  private var testLanguage: SqlTestLanguage? = null

  override fun setupProjectSdksModel(model: ProjectSdksModel) {
    val (jdkPath, sdk) = findBundledJdk(model) ?: return
    if (sdk == null) {
      model.addSdk(JavaSdk.getInstance(), jdkPath) {
        jdk = it
      }
    }
    else {
      jdk = sdk
    }
  }

  override fun getLanguageSettingsComponents(
    course: Course,
    disposable: CheckedDisposable,
    context: UserDataHolder?
  ): List<LabeledComponent<JComponent>> {
    val components = mutableListOf<LabeledComponent<JComponent>>()
    // It doesn't make sense to show a test language component for learners since it doesn't affect course creation anyhow
    if (!course.isStudy) {
      components += createTestLanguageComponent(disposable)
    }

    // Non-null jdk means that `setupProjectSdksModel` successfully found bundled JDK.
    // So there is no reason to show JDK settings at all
    if (jdk == null) {
      components += super.getLanguageSettingsComponents(course, disposable, context)
    }

    return components
  }

  private fun createTestLanguageComponent(disposable: Disposable): LabeledComponent<JComponent> {
    val comboBox: ComboBox<SqlTestLanguage> = ComboBox(comboboxModel())
    val defaultTextLanguage = SqlTestLanguage.KOTLIN.takeIf { it.getLanguage() != null } ?: SqlTestLanguage.JAVA
    comboBox.selectedItem = defaultTextLanguage
    comboBox.renderer = object : DefaultListCellRenderer() {
      override fun getListCellRendererComponent(
        list: JList<*>,
        value: Any?,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
      ): Component {
        val component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
        if (component is JLabel && value is SqlTestLanguage) {
          val language = value.getLanguage()
          if (language != null) {
            component.text = language.displayName
            component.icon = value.logo
          }
        }
        return component
      }

    }

    comboBox.whenItemSelected(disposable) {
      testLanguage = it
    }

    return LabeledComponent.create(comboBox, EduSqlBundle.message("edu.sql.test.language"), BorderLayout.WEST)
  }

  private fun comboboxModel(): ComboBoxModel<SqlTestLanguage> {
    val languages = SqlTestLanguage.entries.filterTo(Vector()) { it.getLanguage() != null }
    return DefaultComboBoxModel(languages)
  }

  override fun getSettings(): JdkProjectSettings = SqlJdkProjectSettings(sdkModel, jdk, testLanguage)
}
