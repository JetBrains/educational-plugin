package com.jetbrains.edu.coursecreator.actions

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.Section
import com.jetbrains.edu.learning.courseFormat.StudyItem
import icons.EducationalCoreIcons

class CCCreateLesson : CCCreateLessonBase<Lesson>(StudyItemType.LESSON, EducationalCoreIcons.Lesson) {
  override fun createAndInitItem(project: Project, course: Course, parentItem: StudyItem?, info: NewStudyItemInfo): Lesson {
    return Lesson().apply {
      this.name = info.name
      this.course = course
      this.index = info.index
      if (parentItem is Section) {
        this.section = parentItem
      }
    }
  }
}
