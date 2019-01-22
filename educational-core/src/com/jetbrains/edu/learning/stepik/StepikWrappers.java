package com.jetbrains.edu.learning.stepik;

import com.google.gson.annotations.Expose;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;

public class StepikWrappers {

  public static class TaskWrapper {
    @Expose Task task;

    public TaskWrapper(Task task) {
      this.task = task;
    }
  }
}
