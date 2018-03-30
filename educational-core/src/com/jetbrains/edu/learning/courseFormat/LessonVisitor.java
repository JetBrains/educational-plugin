package com.jetbrains.edu.learning.courseFormat;

public interface LessonVisitor {

  /**
   * @return true to continue visiting lessons, false -- to abort
   */
  boolean visitLesson(Lesson lesson, int index);
}
