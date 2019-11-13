package com.jetbrains.edu.learning.yaml.format

import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import java.util.*

/**
 * Placeholder to deserialize task type, should be replaced with actual task later
 */
class TaskWithType(title: String) : Task(title, 0, 0, Date(0), CheckStatus.Unchecked) {
  override fun getItemType(): String {
    throw NotImplementedError()
  }
}