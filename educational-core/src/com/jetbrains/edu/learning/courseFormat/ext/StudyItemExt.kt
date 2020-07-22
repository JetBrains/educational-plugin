package com.jetbrains.edu.learning.courseFormat.ext

import com.jetbrains.edu.coursecreator.*
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.Section
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseFormat.tasks.Task

val StudyItem.studyItemType: StudyItemType get() {
  return when (this) {
    is Task -> TaskType
    is Lesson -> LessonType
    is Section -> SectionType
    is Course -> CourseType
    else -> error("Unexpected study item class: ${javaClass.simpleName}")
  }
}
