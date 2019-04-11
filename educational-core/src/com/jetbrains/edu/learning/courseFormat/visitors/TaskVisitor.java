package com.jetbrains.edu.learning.courseFormat.visitors;

import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface TaskVisitor {
  void visit(@NotNull Task task);
}
