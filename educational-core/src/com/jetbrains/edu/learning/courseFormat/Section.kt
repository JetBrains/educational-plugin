package com.jetbrains.edu.learning.courseFormat

import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.EduNames


open class Section : LessonContainer() {

  override fun init(parentItem: ItemContainer, isRestarted: Boolean) {
    require(parentItem is Course) { "Course is null for section $name" }
    super.init(parentItem, isRestarted)
  }

  override fun getDir(baseDir: VirtualFile): VirtualFile? {
    return baseDir.findChild(name)
  }

  override val course: Course
    get() = parent as? Course ?: error("Course is null for section $name")
  override val itemType: String = EduNames.SECTION

}
