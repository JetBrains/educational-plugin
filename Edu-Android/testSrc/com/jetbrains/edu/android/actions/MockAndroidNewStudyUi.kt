package com.jetbrains.edu.android.actions

import com.intellij.openapi.project.Project
import com.jetbrains.edu.android.AndroidCourseBuilder
import com.jetbrains.edu.coursecreator.actions.NewStudyItemInfo
import com.jetbrains.edu.coursecreator.actions.NewStudyItemUiModel
import com.jetbrains.edu.coursecreator.actions.create.MockNewStudyItemUi
import com.jetbrains.edu.coursecreator.ui.AdditionalPanel
import com.jetbrains.edu.learning.courseFormat.Course

class MockAndroidNewStudyUi(
  name: String,
  private val packageName: String,
  index: Int? = null,
  itemType: String? = null
) : MockNewStudyItemUi(name, index, itemType) {
  override fun show(
    project: Project,
    course: Course,
    model: NewStudyItemUiModel,
    additionalPanels: List<AdditionalPanel>,
    studyItemCreator: (NewStudyItemInfo) -> Unit
  ) {
    super.show(project, course, model, additionalPanels) {
      it.putUserData(AndroidCourseBuilder.PACKAGE_NAME, packageName)
      it.putUserData(AndroidCourseBuilder.MIN_ANDROID_SDK, 15)
      it.putUserData(AndroidCourseBuilder.COMPILE_ANDROID_SDK, 28)
      studyItemCreator(it)
    }
  }
}
