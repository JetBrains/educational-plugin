package com.jetbrains.edu.learning.yaml.format

import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse

class RemoteHyperskillChangeApplier : RemoteInfoChangeApplierBase<HyperskillCourse>() {
  override fun applyChanges(existingItem: HyperskillCourse, deserializedItem: HyperskillCourse) {
    existingItem.hyperskillProject = deserializedItem.hyperskillProject
    existingItem.stages = deserializedItem.stages
    existingItem.taskToTopics = deserializedItem.taskToTopics
    existingItem.updateDate = deserializedItem.updateDate
  }
}