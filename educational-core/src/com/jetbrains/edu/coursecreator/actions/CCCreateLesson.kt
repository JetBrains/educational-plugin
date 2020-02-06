package com.jetbrains.edu.coursecreator.actions

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.*
import icons.EducationalCoreIcons

class CCCreateLesson : CCCreateLessonBase<Lesson>(StudyItemType.LESSON, EducationalCoreIcons.Lesson) {

  override val studyItemVariants: List<StudyItemVariant>
    get() = listOf(
      StudyItemVariant("Lesson", "", EducationalCoreIcons.Lesson, ::Lesson),
      StudyItemVariant("Project-based Lesson", "", EducationalCoreIcons.Lesson, ::FrameworkLesson)
    )

  override fun initItem(project: Project, course: Course, parentItem: StudyItem?, item: Lesson, info: NewStudyItemInfo) {
    item.course = course
    if (parentItem is Section) {
      item.section = parentItem
    }
  }
}
