package com.jetbrains.edu.android

import com.android.sdklib.SdkVersionInfo
import com.android.tools.idea.npw.FormFactor
import com.android.tools.idea.npw.model.NewProjectModel
import com.android.tools.idea.npw.platform.AndroidVersionsInfo
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBTextField
import com.intellij.ui.layout.*
import com.jetbrains.edu.android.messages.EduAndroidBundle
import com.jetbrains.edu.coursecreator.actions.NewStudyItemInfo
import com.jetbrains.edu.coursecreator.actions.NewStudyItemUiModel
import com.jetbrains.edu.coursecreator.ui.CCCreateStudyItemDialogBase
import com.jetbrains.edu.learning.courseFormat.Course
import org.jetbrains.android.util.AndroidUtils
import java.awt.Component
import java.util.function.Consumer
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.ListCellRenderer

class AndroidNewTaskAfterPopupDialog(
  project: Project,
  course: Course,
  model: NewStudyItemUiModel,
  private val currentInfo: NewStudyItemInfo
) : CCCreateStudyItemDialogBase(project, course, model, emptyList()) {

  init { init() }

  private val packageNameField: JBTextField = JBTextField(TEXT_FIELD_COLUMNS).apply {
    val userName = System.getProperty("user.name")
    val packageSuffix = if (userName != null) NewProjectModel.nameToJavaPackage(userName) else null
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

  private var compileSdkVersion: Int = SdkVersionInfo.HIGHEST_KNOWN_STABLE_API

  override fun showNameField(): Boolean = false
  override fun createNewStudyItemInfo(): NewStudyItemInfo = currentInfo

  override fun createAdditionalFields(builder: LayoutBuilder) {
    val androidVersionsInfo = AndroidVersionsInfo()
    androidVersionsInfo.loadLocalVersions()
    androidVersionsInfo.loadRemoteTargetVersions(FormFactor.MOBILE, FormFactor.MOBILE.minOfflineApiLevel, Consumer { items ->
      val nonPreviewItems = items.filter { it.androidTarget?.version?.isPreview != true }
      val maxSdkVersion = nonPreviewItems.map { it.minApiLevel }.max() ?: SdkVersionInfo.HIGHEST_KNOWN_STABLE_API
      compileSdkVersion = maxOf(maxSdkVersion, compileSdkVersion)
      comboBoxWrapper.init(FormFactor.MOBILE, nonPreviewItems)
      comboBoxWrapper.combobox.isEnabled = true
    })
    addTextValidator(packageNameField) { text ->
      if (text == null) return@addTextValidator EduAndroidBundle.message("empty.package")
      AndroidUtils.validateAndroidPackageName(text)
    }

    with(builder) {
      row(EduAndroidBundle.message("package.row")) { packageNameField() }
      row(EduAndroidBundle.message("min.sdk.row")) { comboBoxWrapper.combobox(CCFlags.growX) }
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
      component.text = EduAndroidBundle.message("loading")
    }
    return component
  }
}
