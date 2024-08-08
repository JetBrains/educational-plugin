package com.jetbrains.edu.learning.courseFormat

enum class DescriptionFormat(val extension: String) {
  HTML("html"),
  MD("md");

  val fileName: String
    get() = "$TASK_DESCRIPTION_PREFIX.${extension}"

  companion object {
    const val TASK_DESCRIPTION_PREFIX: String = "task"
  }
}
