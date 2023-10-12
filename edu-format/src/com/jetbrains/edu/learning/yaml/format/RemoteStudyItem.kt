package com.jetbrains.edu.learning.yaml.format

import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ItemContainer
import com.jetbrains.edu.learning.courseFormat.StudyItem

/**
 * Placeholder to deserialize [StudyItem] with remote info. Remote info is applied to
 * existing StudyItem by [com.jetbrains.edu.learning.yaml.format.RemoteInfoChangeApplierBase]
 */
class RemoteStudyItem : StudyItem() {

  override fun init(parentItem: ItemContainer, isRestarted: Boolean) {
    throw NotImplementedError()
  }

  override val course: Course
    get() = throw NotImplementedError()
  override val itemType: String
    get() = throw NotImplementedError()

}