package com.jetbrains.edu.learning.yaml.format

import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.StudyItem

/**
 * Placeholder for any StudyItem, should be filled with actual content later
 */
class TitledStudyItem(title: String) : StudyItem(title) {

  override fun init(course: Course?, parentItem: StudyItem?, isRestarted: Boolean) {
    throw NotImplementedError()
  }

  override fun getDir(baseDir: VirtualFile): VirtualFile? {
    throw NotImplementedError()
  }

  override val course: Course
    get() = throw NotImplementedError()
  override val itemType: String
    get() = throw NotImplementedError()
}