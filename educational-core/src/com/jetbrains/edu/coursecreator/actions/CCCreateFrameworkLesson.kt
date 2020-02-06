package com.jetbrains.edu.coursecreator.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.util.BuildNumber
import com.jetbrains.edu.learning.EduExperimentalFeatures
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.isFeatureEnabled
import com.jetbrains.edu.learning.messages.EduCoreBundle
import icons.EducationalCoreIcons

// BACKCOMPAT: 2019.2. Drop it
@Suppress("ComponentNotRegistered")  // educational-core.xml
class CCCreateFrameworkLesson :
  CCCreateLessonBase<FrameworkLesson>(StudyItemType.FRAMEWORK_LESSON, EducationalCoreIcons.Lesson) {

  override fun update(event: AnActionEvent) {
    // Don't show this action if new popup-based dialog is enabled
    if (ApplicationInfo.getInstance().build >= BUILD_193 && isFeatureEnabled(EduExperimentalFeatures.NEW_ITEM_POPUP_UI)) {
      event.presentation.isEnabledAndVisible = false
      return
    }
    super.update(event)
  }

  override val studyItemVariants: List<StudyItemVariant>
    get() = listOf(
      StudyItemVariant(EduCoreBundle.message("study.item.framework.lesson"), "", EducationalCoreIcons.Lesson, ::FrameworkLesson)
    )

  companion object {
    private val BUILD_193: BuildNumber = BuildNumber.fromString("193")
  }
}
