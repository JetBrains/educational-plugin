package com.jetbrains.edu.learning.yaml.format

import com.jetbrains.edu.learning.courseFormat.stepik.StepikLesson

class StepikLessonChangeApplier : RemoteInfoChangeApplierBase<StepikLesson>() {
  override fun applyChanges(existingItem: StepikLesson, deserializedItem: StepikLesson) {
    super.applyChanges(existingItem, deserializedItem)
    existingItem.unitId = deserializedItem.unitId
  }
}