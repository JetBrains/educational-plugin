package com.jetbrains.edu.learning.courseFormat.ext

import com.jetbrains.edu.coursecreator.StudyItemType
import com.jetbrains.edu.coursecreator.StudyItemType.*
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.Section
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseFormat.tasks.Task

val StudyItem.studyItemType: StudyItemType get() {
  return when (this) {
    is Task -> TASK_TYPE
    is Lesson -> LESSON_TYPE
    is Section -> SECTION_TYPE
    is Course -> COURSE_TYPE
    else -> error("Unexpected study item class: ${javaClass.simpleName}")
  }
}
