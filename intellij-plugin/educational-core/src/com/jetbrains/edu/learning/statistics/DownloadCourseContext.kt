package com.jetbrains.edu.learning.statistics

/**
 * Describes a reason why the plugin downloads a course.
 * It's mostly used to calculate proper download statistics
 */
enum class DownloadCourseContext {
  /**
   * A user decided to open a course via `Browse Courses` dialog or with another specific action inside IDE
   */
  IDE_UI,

  /**
   * A course is downloaded to make content update for an already opened course project
   */
  UPDATE,

  /**
   * A course was opened with a button in the browser, like `Open in <IDE>` button on Marketplace course page.
   *
   * Similar with [TOOLBOX] but the corresponding course open workflow doesn't involve Toolbox App,
   * i.e., browser interacts with IDE directly
   */
  WEB,

  /**
   * A course was opened using Toolbox App [workflow](https://youtrack.jetbrains.com/issue/EDU-6628).
   * In most cases, the corresponding course open workflow also uses Web as initial point,
   * but interaction with IDE is done via Toolbox App, not directly like in case of [WEB]
   */
  TOOLBOX,

  /**
   * Another technical reason to download course.
   * Use it when you don't care about statistics
   */
  OTHER;

  override fun toString(): String = name.lowercase()
}
