package com.jetbrains.edu.learning.courseFormat

import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.EduNames


open class Section : LessonContainer() {

  // move to stepik
  @Transient
  var units: List<Int> = listOf()
  var position = 0

  override fun init(course: Course?, parentItem: StudyItem?, isRestarted: Boolean) {
    require(course is Course) { "Course is null for section $name" }
    parent = course

    for ((i, lesson) in lessons.withIndex()) {
      lesson.index = i + 1
      lesson.init(course, this, isRestarted)
    }
  }

  override fun getDir(baseDir: VirtualFile): VirtualFile? {
    return baseDir.findChild(name)
  }

  override val course: Course
    get() = parent as? Course ?: error("Course is null for section $name")
  override val itemType: String = EduNames.SECTION

}
