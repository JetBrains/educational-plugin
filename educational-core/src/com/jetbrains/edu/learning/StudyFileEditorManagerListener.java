package com.jetbrains.edu.learning;

import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.edu.learning.courseFormat.tasks.ChoiceTask;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.editor.ChoiceVariantsPanel;
import com.jetbrains.edu.learning.ui.taskDescription.StudyToolWindow;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class StudyFileEditorManagerListener implements FileEditorManagerListener {
  private final StudyToolWindow myToolWindow;
  private final Project myProject;

  public StudyFileEditorManagerListener(StudyToolWindow toolWindow, Project project) {
    myToolWindow = toolWindow;
    myProject = project;
  }

  @Override
  public void selectionChanged(@NotNull FileEditorManagerEvent event) {
    VirtualFile file = event.getNewFile();
    Task task = null;
    if (file != null) {
      task = getTask(file);
    }
    myToolWindow.setCurrentTask(myProject, task);
    if (task instanceof ChoiceTask) {
      final ChoiceVariantsPanel choicePanel = new ChoiceVariantsPanel((ChoiceTask) task);
      myToolWindow.setBottomComponent(choicePanel);
    } else {
      myToolWindow.setBottomComponent(null);
    }
  }

  @Nullable
  private Task getTask(@NotNull VirtualFile file) {
    return StudyUtils.getTaskForFile(myProject, file);
  }
}