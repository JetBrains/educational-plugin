package com.jetbrains.edu.coursecreator.configuration.mixins

import com.jetbrains.edu.learning.courseFormat.tasks.Task

/**
 * Placeholder to deserialize task type, should be replaced with actual task later
 */
class TaskWithType(title: String) : Task(title) {
  override fun getTaskType(): String {
    throw NotImplementedError()
  }
}