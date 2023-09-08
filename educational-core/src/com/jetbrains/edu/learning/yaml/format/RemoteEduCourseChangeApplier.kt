package com.jetbrains.edu.learning.yaml.format

import com.jetbrains.edu.learning.courseFormat.EduCourse

class RemoteEduCourseChangeApplier : RemoteInfoChangeApplierBase<EduCourse>() {
  override fun applyChanges(existingItem: EduCourse, deserializedItem: EduCourse) {
    super.applyChanges(existingItem, deserializedItem)
    existingItem.sectionIds = deserializedItem.sectionIds
    existingItem.marketplaceCourseVersion = deserializedItem.marketplaceCourseVersion
    existingItem.generatedEduId = deserializedItem.generatedEduId
  }
}
