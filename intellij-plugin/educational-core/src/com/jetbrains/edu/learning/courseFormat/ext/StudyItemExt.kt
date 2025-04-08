package com.jetbrains.edu.learning.courseFormat.ext

import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.coursecreator.StudyItemType
import com.jetbrains.edu.coursecreator.StudyItemType.*
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import java.nio.file.Paths

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
    is Section -> {
      val relativePathToItems = Paths.get(course.contentShift, name).toString()
      courseDir.findFileByRelativePath(relativePathToItems)
    }

    is Lesson -> {
      val relativePathToItems = if (parent is Course) {
        Paths.get(course.contentShift, name).toString()
      }
      else name
      parent.getDir(courseDir)?.findFileByRelativePath(relativePathToItems)
    }

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