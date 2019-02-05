package com.jetbrains.edu.learning.framework;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public interface FrameworkLessonManager {
  void prepareNextTask(@NotNull FrameworkLesson lesson, @NotNull VirtualFile taskDir);
  void preparePrevTask(@NotNull FrameworkLesson lesson, @NotNull VirtualFile taskDir);

  void saveSolution(@NotNull Task task, @NotNull Map<String, String> solutions);

  @NotNull
  static FrameworkLessonManager getInstance(@NotNull Project project) {
    return ServiceManager.getService(project, FrameworkLessonManager.class);
  }
}
