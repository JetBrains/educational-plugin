package com.jetbrains.edu.android.actions

import com.intellij.openapi.project.Project
import com.jetbrains.edu.android.AndroidCourseBuilder
import com.jetbrains.edu.coursecreator.actions.create.MockNewStudyItemUi
import com.jetbrains.edu.coursecreator.actions.studyItem.NewStudyItemInfo
import com.jetbrains.edu.coursecreator.actions.studyItem.NewStudyItemUiModel
import com.jetbrains.edu.coursecreator.ui.NewStudyItemInfoCallback
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
    callback: NewStudyItemInfoCallback
  ) {
    val newStudyItemCreator: (NewStudyItemInfo) -> Unit = {
      it.putUserData(AndroidCourseBuilder.PACKAGE_NAME, packageName)
      it.putUserData(AndroidCourseBuilder.MIN_ANDROID_SDK, 15)
      it.putUserData(AndroidCourseBuilder.COMPILE_ANDROID_SDK, 28)
      callback.studyItemCreator(it)
    }

    val newCallback = NewStudyItemInfoCallback(callback.validator, newStudyItemCreator)
    super.show(project, course, model, newCallback)
  }
}
