package com.jetbrains.edu.learning.courseFormat

enum class DescriptionFormat(val extension: String) {
  HTML("html"),
  MD("md");

  val fileName: String
    get() = "$TASK_DESCRIPTION_PREFIX.${extension}"

  companion object {
    const val TASK_DESCRIPTION_PREFIX: String = "task"

    val taskDescriptionRegex: Regex =
      """^$TASK_DESCRIPTION_PREFIX(?:_[a-z]{2}(?:-[A-Z]{2})?)?\.(${HTML.extension}|${MD.extension})$""".toRegex()
  }
}
