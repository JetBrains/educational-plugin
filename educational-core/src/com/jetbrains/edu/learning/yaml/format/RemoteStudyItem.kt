package com.jetbrains.edu.learning.yaml.format

import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ItemContainer
import com.jetbrains.edu.learning.courseFormat.StudyItem

/**
 * Placeholder to deserialize [StudyItem] with remote info. Remote info is applied to
 * existing StudyItem by [RemoteInfoChangeApplierBase]
 */
class RemoteStudyItem : StudyItem() {

  override fun init(course: Course?, parentItem: StudyItem?, isRestarted: Boolean) {
    throw NotImplementedError()
  }
  override fun getDir(baseDir: VirtualFile): VirtualFile? {
    throw NotImplementedError()
  }

  override val course: Course
    get() = throw NotImplementedError()
  override val parent: ItemContainer
    get() = throw NotImplementedError()
  override val itemType: String
    get() = throw NotImplementedError()

}