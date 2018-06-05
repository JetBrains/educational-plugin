package com.jetbrains.edu.learning.courseFormat

/**
 * Stepik item status, that indicates whether it needs an update
 */
enum class StepikChangeStatus {

  /**
   * Uses to mark:
   * changed [com.jetbrains.edu.learning.courseFormat.tasks.Task];
   * [Course], [Section], [Lesson] if [INFO] and [CONTENT] were changed both.
   */
  INFO_AND_CONTENT,

  /**
   * Used for [Course], [Section], [Lesson]. Indicates that some information, e.g. name, position, has been changed
   */
  INFO,

  /**
   * Used for [Course], [Section], [Lesson], indicates that its elements number has been changed, e.g. a lesson has been deleted.
   */
  CONTENT,

  UP_TO_DATE
}

