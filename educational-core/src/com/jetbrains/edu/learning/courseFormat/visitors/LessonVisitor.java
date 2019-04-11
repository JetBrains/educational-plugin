package com.jetbrains.edu.learning.courseFormat.visitors;

import com.jetbrains.edu.learning.courseFormat.Lesson;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface LessonVisitor {
  void visit(@NotNull Lesson lesson);
}
