package com.jetbrains.edu.learning.courseFormat;

import org.jetbrains.annotations.NotNull;

public interface LessonVisitor {

  /**
   * @return true to continue visiting lessons, false -- to abort
   */
  boolean visitLesson(@NotNull Lesson lesson, int index);
}
