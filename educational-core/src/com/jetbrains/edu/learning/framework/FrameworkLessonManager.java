package com.jetbrains.edu.learning.framework;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public interface FrameworkLessonManager {
  void prepareNextTask(@NotNull FrameworkLesson lesson, @NotNull VirtualFile taskDir, boolean showDialogIfConflict);
  void preparePrevTask(@NotNull FrameworkLesson lesson, @NotNull VirtualFile taskDir, boolean showDialogIfConflict);

  void saveExternalChanges(@NotNull Task task, @NotNull Map<String, String> externalState);
  void updateUserChanges(@NotNull Task task, @NotNull Map<String, String> newInitialState);

  @NotNull
  static FrameworkLessonManager getInstance(@NotNull Project project) {
    return ServiceManager.getService(project, FrameworkLessonManager.class);
  }
}
