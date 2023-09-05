package com.jetbrains.edu.learning.yaml.format

import com.jetbrains.edu.learning.courseFormat.tasks.DataTask

class RemoteDataTaskChangeApplier : RemoteInfoChangeApplierBase<DataTask>() {
  override fun applyChanges(existingItem: DataTask, deserializedItem: DataTask) {
    super.applyChanges(existingItem, deserializedItem)
    existingItem.attempt = deserializedItem.attempt
  }
}