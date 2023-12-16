package com.jetbrains.edu.learning.courseFormat.ext

import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.coursecreator.StudyItemType
import com.jetbrains.edu.coursecreator.StudyItemType.*
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.Lesson
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
    is Task -> {
      val lessonDir = lesson.getDir(courseDir)
      if (lessonDir == null) {
        error("Can't find lesson directory for ")
      }
      else {
        findDir(lessonDir)
      }
    }
    else -> error("Can't find directory for the item $itemType")
  }
}

fun StudyItem.getPathInCourse(): String {
  val parents = mutableListOf<String>()
  var currentParent = parent
  while (currentParent !is Course) {
    parents.add(currentParent.name)
    currentParent = currentParent.parent
  }
  parents.reverse()
  if (parents.isEmpty()) return name
  parents.add(name)
  return parents.joinToString(VfsUtilCore.VFS_SEPARATOR)
}