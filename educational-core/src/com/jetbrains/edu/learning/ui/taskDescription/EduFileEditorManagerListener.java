package com.jetbrains.edu.learning.ui.taskDescription;

import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import org.jetbrains.annotations.NotNull;

public class EduFileEditorManagerListener implements FileEditorManagerListener {
  private final Project myProject;

  public EduFileEditorManagerListener(Project project) {
    myProject = project;
  }

  @Override
  public void selectionChanged(@NotNull FileEditorManagerEvent event) {
    VirtualFile file = event.getNewFile();
    Task task = null;
    if (file != null) {
      task = EduUtils.getTaskForFile(myProject, file);
    }
    TaskDescriptionView.getInstance(myProject).setCurrentTask(task);
  }
}