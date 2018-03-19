package com.jetbrains.edu.learning.courseFormat;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface LessonVisitor {

  /**
   * @return true to continue visiting lessons, false -- to abort
   */
  boolean visit(@NotNull Lesson lesson);
}
