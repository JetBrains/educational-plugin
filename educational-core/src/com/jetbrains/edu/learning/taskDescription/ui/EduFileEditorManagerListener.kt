package com.jetbrains.edu.learning.taskDescription.ui;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.edu.learning.VirtualFileExt;
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
      task = VirtualFileExt.getContainingTask(file, myProject);
    }
    if (task != null) {
      TaskDescriptionView.getInstance(myProject).setCurrentTask(task);
    }
  }

  @Override
  public void fileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
    if (FileEditorManager.getInstance(myProject).getOpenFiles().length == 0) {
      TaskDescriptionView.getInstance(myProject).setCurrentTask(null);
    }
  }
}