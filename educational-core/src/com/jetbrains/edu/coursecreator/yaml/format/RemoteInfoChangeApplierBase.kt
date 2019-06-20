package com.jetbrains.edu.coursecreator.yaml.format

import com.jetbrains.edu.coursecreator.yaml.loadingError
import com.jetbrains.edu.coursecreator.yaml.unexpectedItemTypeMessage
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.StudyItem

open class RemoteInfoChangeApplierBase<T : StudyItem> : StudyItemChangeApplier<T>() {
  override fun applyChanges(existingItem: T, deserializedItem: T) {
    existingItem.id = deserializedItem.id
    existingItem.updateDate = deserializedItem.updateDate
  }
}

fun <T : StudyItem> getRemoteChangeApplierForItem(item: T): RemoteInfoChangeApplierBase<T> {
  @Suppress("UNCHECKED_CAST") //
  return when (item) {
    is EduCourse -> RemoteCourseChangeApplier()
    is Lesson -> RemoteLessonChangeApplier()
    is RemoteStudyItem -> RemoteInfoChangeApplierBase<T>()
    else -> loadingError(unexpectedItemTypeMessage(item.javaClass.simpleName))
  } as RemoteInfoChangeApplierBase<T>
}