package com.jetbrains.edu.coursecreator.yaml.format

import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.Section
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseFormat.tasks.Task

open class RemoteInfoChangeApplierBase<T : StudyItem> : StudyItemChangeApplier<T>() {
  override fun applyChanges(existingItem: T, deserializedItem: T) {
    existingItem.id = deserializedItem.id
  }
}

fun <T : StudyItem> getRemoteChangeApplierForItem(item: T): RemoteInfoChangeApplierBase<T> {
  @Suppress("UNCHECKED_CAST") //
  return when (item) {
    is EduCourse -> RemoteCourseChangeApplier<EduCourse>()
    is Section, is Lesson, is Task -> RemoteInfoChangeApplierBase<T>()
    else -> error("Unexpected item type: ${item.javaClass.simpleName}")
  } as RemoteInfoChangeApplierBase<T>
}