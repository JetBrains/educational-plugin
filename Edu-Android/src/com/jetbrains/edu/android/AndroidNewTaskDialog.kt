package com.jetbrains.edu.android

import com.android.tools.idea.npw.FormFactor
import com.android.tools.idea.npw.model.NewProjectModel
import com.android.tools.idea.npw.platform.AndroidVersionsInfo
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBTextField
import com.intellij.ui.layout.*
import com.jetbrains.edu.coursecreator.actions.NewStudyItemInfo
import com.jetbrains.edu.coursecreator.actions.NewStudyItemUiModel
import com.jetbrains.edu.coursecreator.ui.AdditionalPanel
import com.jetbrains.edu.coursecreator.ui.CCCreateStudyItemDialogBase
import org.jetbrains.android.util.AndroidUtils
import java.awt.Component
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.ListCellRenderer

class AndroidNewTaskDialog(
  project: Project,
  model: NewStudyItemUiModel,
  additionalPanels: List<AdditionalPanel>
) : CCCreateStudyItemDialogBase(project, model, additionalPanels) {

  private val packageNameField: JBTextField = JBTextField().apply {
    val userName = System.getProperty("user.name")
    val packageSuffix = if (userName != null) NewProjectModel.toPackagePart(userName) else null
    text = if (packageSuffix.isNullOrEmpty()) {
      "com.example.android.course"
    } else {
      "com.example.$packageSuffix"
    }
  }

  private val comboBoxWrapper: AndroidApiLevelComboBoxWrapper = AndroidApiLevelComboBoxWrapper().apply {
    combobox.renderer = LoadingRenderer(combobox.renderer)
    combobox.isEnabled = false
  }

  private var compileSdkVersion: Int = FormFactor.MOBILE.maxOfflineApiLevel

  init { init() }

  override fun createAdditionalFields(builder: LayoutBuilder) {
    val androidVersionsInfo = AndroidVersionsInfo()
    androidVersionsInfo.loadTargetVersions { items ->
      val maxSdkVersion = items.map { it.minApiLevel }.max() ?: FormFactor.MOBILE.maxOfflineApiLevel
      compileSdkVersion = maxOf(maxSdkVersion, compileSdkVersion)
      comboBoxWrapper.init(FormFactor.MOBILE, items)
      comboBoxWrapper.combobox.isEnabled = true
    }
    addTextValidator(packageNameField) { text ->
      if (text == null) return@addTextValidator "Empty package"
      AndroidUtils.validateAndroidPackageName(text)
    }

    with(builder) {
      row("Package:") { packageNameField() }
      row("Min Sdk:") { comboBoxWrapper.combobox(CCFlags.growX) }
    }
  }

  override fun showAndGetResult(): NewStudyItemInfo? {
    val info = super.showAndGetResult()
    val versionItem = comboBoxWrapper.combobox.selectedItem as? AndroidVersionsInfo.VersionItem
    return info?.apply {
      putUserData(AndroidCourseBuilder.PACKAGE_NAME, packageNameField.text)
      putUserData(AndroidCourseBuilder.MIN_ANDROID_SDK, versionItem?.minApiLevel ?: FormFactor.MOBILE.minOfflineApiLevel)
      putUserData(AndroidCourseBuilder.COMPILE_ANDROID_SDK, compileSdkVersion)
    }
  }
}

private class LoadingRenderer<E>(private val delegate: ListCellRenderer<E>) : ListCellRenderer<E> {

  override fun getListCellRendererComponent(list: JList<out E>?, value: E?, index: Int, isSelected: Boolean, cellHasFocus: Boolean): Component {
    val component = delegate.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
    if (value == null && component is JLabel) {
      component.text = "Loading..."
    }
    return component
  }
}
