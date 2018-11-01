package com.jetbrains.edu.coursecreator.actions

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.Section
import com.jetbrains.edu.learning.courseFormat.StudyItem
import icons.EducationalCoreIcons

@Suppress("ComponentNotRegistered")  // educational-core.xml
class CCCreateFrameworkLesson :
  CCCreateLessonBase<FrameworkLesson>(StudyItemType.FRAMEWORK_LESSON, EducationalCoreIcons.Lesson) {

  override fun createAndInitItem(project: Project, course: Course, parentItem: StudyItem?, info: NewStudyItemInfo): FrameworkLesson {
    return FrameworkLesson().apply {
      this.name = info.name
      this.course = course
      this.index = info.index
      if (parentItem is Section) {
        this.section = parentItem
      }
    }
  }
}
