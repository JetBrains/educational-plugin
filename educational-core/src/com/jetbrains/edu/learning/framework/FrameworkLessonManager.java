package com.jetbrains.edu.learning.framework;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson;
import org.jetbrains.annotations.NotNull;

public interface FrameworkLessonManager {
  void prepareNextTask(@NotNull FrameworkLesson lesson, @NotNull VirtualFile taskDir);
  void preparePrevTask(@NotNull FrameworkLesson lesson, @NotNull VirtualFile taskDir);

  @NotNull
  static FrameworkLessonManager getInstance(@NotNull Project project) {
    return ServiceManager.getService(project, FrameworkLessonManager.class);
  }
}
