package com.jetbrains.edu.learning.courseFormat

enum class EduFileErrorHighlightLevel {

  /**
   * Highlight only keywords
   */
  NONE,

  /**
   * Highlight both syntax and inspections.
   */
  ALL_PROBLEMS,

  /**
   * This level is the same as [ALL_PROBLEMS], but syntax errors are temporarily not highlighted.
   * This level is used for files that were just created and still had not been touched by a student.
   * Such files usually have placeholders filled with syntactically incorrect texts, so without the suppression, there would be too many
   * syntactic errors with a lot of highlighting.
   *
   * This level will automatically be changed to [ALL_PROBLEMS] after the file is modified.
   *
   * Syntax errors highlighting is suppressed here: [com.jetbrains.edu.learning.editor.EduHighlightErrorFilter].
   *
   * Additional suppression may be done, as is in `com.jetbrains.edu.python.learning.highlighting.PyEduInspectionExtension`
   */
  TEMPORARY_SUPPRESSION

}