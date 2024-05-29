package com.jetbrains.edu.learning.courseFormat.ext

import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.coursecreator.StudyItemType
import com.jetbrains.edu.coursecreator.StudyItemType.COURSE_TYPE
import com.jetbrains.edu.coursecreator.StudyItemType.LESSON_TYPE
import com.jetbrains.edu.coursecreator.StudyItemType.SECTION_TYPE
import com.jetbrains.edu.coursecreator.StudyItemType.TASK_TYPE
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.LessonContainer
import com.jetbrains.edu.learning.courseFormat.Section
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseFormat.tasks.Task

val StudyItem.studyItemType: StudyItemType
  get() {
    return when (this) {
      is Task -> TASK_TYPE
      is Lesson -> LESSON_TYPE
      is Section -> SECTION_TYPE
      is Course -> COURSE_TYPE
      else -> error("Unexpected study item class: ${javaClass.simpleName}")
    }
  }

fun StudyItem.getDir(courseDir: VirtualFile): VirtualFile? {
  return when (this) {
    is Course -> courseDir
    is Section -> courseDir.findChild(name)
    is Lesson -> parent.getDir(courseDir)?.findChild(name)
    is Task -> findDir(lesson.getDir(courseDir))
    else -> error("Can't find directory for the item $itemType")
  }
}

fun StudyItem.visitTasks(action: (Task) -> Unit) {
  when (this) {
    is LessonContainer -> visitTasks(action)
    is Lesson -> visitTasks(action)
    is Task -> action(this)
  }
}