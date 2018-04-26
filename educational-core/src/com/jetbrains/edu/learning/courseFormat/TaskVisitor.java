package com.jetbrains.edu.learning.courseFormat;

import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface TaskVisitor {
  /**
   * @return true to continue visiting tasks, false -- to abort
   */
  boolean visitTask(@NotNull Task task, int index);
}
