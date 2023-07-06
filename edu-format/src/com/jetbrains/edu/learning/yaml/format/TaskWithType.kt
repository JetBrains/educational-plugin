package com.jetbrains.edu.learning.yaml.format

import com.jetbrains.edu.learning.courseFormat.tasks.Task

/**
 * Placeholder to deserialize task type, should be replaced with actual task later
 */
class TaskWithType(title: String) : Task(title) {
  override val itemType: String
    get() = throw NotImplementedError()
}