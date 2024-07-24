package com.jetbrains.edu.learning.yaml.format

import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.courseFormat.stepik.StepikLesson
import com.jetbrains.edu.learning.courseFormat.tasks.DataTask
import com.jetbrains.edu.learning.yaml.errorHandling.loadingError
import com.jetbrains.edu.learning.yaml.errorHandling.unexpectedItemTypeMessage

open class RemoteInfoChangeApplierBase<T : StudyItem> : StudyItemChangeApplier<T>() {
  override fun applyChanges(existingItem: T, deserializedItem: T) {
    existingItem.id = deserializedItem.id
    existingItem.updateDate = deserializedItem.updateDate
  }
}

fun <T : StudyItem> getRemoteChangeApplierForItem(item: T): RemoteInfoChangeApplierBase<T> {
  @Suppress("UNCHECKED_CAST")
  return when (item) {
    is HyperskillCourse -> RemoteHyperskillChangeApplier()
    is EduCourse -> RemoteEduCourseChangeApplier()
    is StepikLesson -> StepikLessonChangeApplier()
    is DataTask -> RemoteDataTaskChangeApplier()
    is RemoteStudyItem -> RemoteInfoChangeApplierBase<T>()
    else -> loadingError(unexpectedItemTypeMessage(item.javaClass.simpleName))
  } as RemoteInfoChangeApplierBase<T>
}
