package com.jetbrains.edu.android

import com.android.sdklib.SdkVersionInfo
import com.android.tools.adtui.device.FormFactor
import com.android.tools.idea.npw.model.NewProjectModel
import com.android.tools.idea.npw.platform.AndroidVersionsInfo
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.Panel
import com.jetbrains.edu.android.AndroidCourseBuilder.Companion.DEFAULT_PACKAGE_NAME
import com.jetbrains.edu.android.AndroidCourseBuilder.Companion.initAndroidProperties
import com.jetbrains.edu.android.messages.EduAndroidBundle
import com.jetbrains.edu.coursecreator.actions.studyItem.NewStudyItemInfo
import com.jetbrains.edu.coursecreator.actions.studyItem.NewStudyItemUiModel
import com.jetbrains.edu.coursecreator.ui.CCCreateStudyItemDialogBase
import com.jetbrains.edu.learning.courseFormat.Course
import org.jetbrains.android.util.AndroidUtils
import java.awt.Component
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.ListCellRenderer

class AndroidNewTaskAfterPopupDialog(
  project: Project,
  course: Course,
  model: NewStudyItemUiModel,
  private val currentInfo: NewStudyItemInfo
) : CCCreateStudyItemDialogBase(project, course, model) {

  private val packageNameField: JBTextField = JBTextField(TEXT_FIELD_COLUMNS).apply {
    val userName = System.getProperty("user.name")
    val packageSuffix = if (userName != null) NewProjectModel.nameToJavaPackage(userName) else null
    text = if (packageSuffix.isNullOrEmpty()) {
      DEFAULT_PACKAGE_NAME
    }
    else {
      "com.example.$packageSuffix"
    }
  }

  private val comboBoxWrapper: AndroidApiLevelComboBoxWrapper = AndroidApiLevelComboBoxWrapper().apply {
    combobox.renderer = LoadingRenderer(combobox.renderer)
    combobox.isEnabled = false
  }

  private var compileSdkVersion: Int = SdkVersionInfo.HIGHEST_KNOWN_STABLE_API

  init {
    init()
  }

  override fun showNameField(): Boolean = false

  override fun validate(componentText: String?): String? {
      return if (componentText == null) EduAndroidBundle.message("error.no.package")
      else AndroidUtils.validateAndroidPackageName(componentText)
  }

  override fun createNewStudyItemInfo(): NewStudyItemInfo = currentInfo

  override fun createAdditionalFields(panel: Panel) {
    val androidVersionsInfo = AndroidVersionsInfo()
    androidVersionsInfo.loadLocalVersions()
    androidVersionsInfo.loadRemoteTargetVersions(FormFactor.MOBILE, FormFactor.MOBILE.minOfflineApiLevel) { items ->
      val nonPreviewItems = items.filter { it.androidTarget?.version?.isPreview != true }
      val maxSdkVersion = nonPreviewItems.maxOfOrNull { it.minApiLevel } ?: SdkVersionInfo.HIGHEST_KNOWN_STABLE_API
      compileSdkVersion = maxOf(maxSdkVersion, compileSdkVersion)
      comboBoxWrapper.init(FormFactor.MOBILE, nonPreviewItems)
      comboBoxWrapper.combobox.isEnabled = true
    }
    addTextValidator(packageNameField)

    with(panel) {
      row(EduAndroidBundle.message("package.colon")) {
        cell(packageNameField)
          .align(AlignX.FILL)
      }
      row(EduAndroidBundle.message("min.sdk.colon")) {
        cell(comboBoxWrapper.combobox)
          .align(AlignX.FILL)
      }
    }
  }

  override fun showAndGetResult(): NewStudyItemInfo? {
    return super.showAndGetResult()?.apply {
      val versionItem = comboBoxWrapper.combobox.selectedItem as? AndroidVersionsInfo.VersionItem
      initAndroidProperties(compileSdkVersion, packageNameField.text, versionItem?.minApiLevel)
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
