package com.jetbrains.edu.learning.courseFormat

import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.EduNames


open class Section : LessonContainer() {
  @Transient
  private var myCourse: Course? = null

  // move to stepik
  @Transient
  var units: List<Int> = listOf()
  var position = 0

  override fun init(course: Course?, parentItem: StudyItem?, isRestarted: Boolean) {
    myCourse = course

    for ((i, lesson) in lessons.withIndex()) {
      lesson.index = i + 1
      lesson.init(course, this, isRestarted)
    }
  }

  override fun getDir(baseDir: VirtualFile): VirtualFile? {
    return baseDir.findChild(name)
  }

  override val course: Course
    get() = myCourse ?: error("Course is null for section $name")
  override val parent: ItemContainer
    get() = course
  override val itemType: String = EduNames.SECTION

  fun setCourse(course: Course?) {
    myCourse = course
  }

}
