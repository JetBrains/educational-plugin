package com.jetbrains.edu.learning.projectView;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import com.intellij.ui.JBColor;
import com.jetbrains.edu.learning.EduNames;
import com.jetbrains.edu.learning.courseFormat.CheckStatus;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.courseFormat.tasks.TaskWithSubtasks;
import com.jetbrains.edu.learning.navigation.NavigationUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class TaskNode extends EduNode {
  @NotNull protected final Task myTask;

  public TaskNode(@NotNull Project project,
                  PsiDirectory value,
                  ViewSettings viewSettings,
                  @NotNull Task task) {
    super(project, value, viewSettings);
    myTask = task;
  }

  @Override
  public int getWeight() {
    return myTask.getIndex();
  }

  @Override
  protected void updateImpl(PresentationData data) {
    CheckStatus status = myTask.getStatus();
    String subtaskInfo = myTask instanceof TaskWithSubtasks ? getSubtaskInfo((TaskWithSubtasks)myTask) : null;
    Icon icon = myTask.getIcon();
    if (status == CheckStatus.Unchecked) {
      updatePresentation(data, myTask.getPresentableName(), JBColor.BLACK, icon, subtaskInfo);
      return;
    }
    boolean isSolved = status == CheckStatus.Solved;
    JBColor color = isSolved ? LIGHT_GREEN : JBColor.RED;
    updatePresentation(data, myTask.getPresentableName(), color, icon, subtaskInfo);
  }

  private static String getSubtaskInfo(TaskWithSubtasks task) {
    int index = task.getActiveSubtaskIndex() + 1;
    int subtasksNum = task.getLastSubtaskIndex() + 1;
    return EduNames.SUBTASK + " " + index + "/" + subtasksNum;
  }

  @Override
  public boolean expandOnDoubleClick() {
    return false;
  }

  @Override
  public boolean canNavigate() {
    return true;
  }

  @Override
  public void navigate(boolean requestFocus) {
    NavigationUtils.navigateToTask(myProject, myTask);
  }

  @Override
  @Nullable
  public AbstractTreeNode modifyChildNode(AbstractTreeNode childNode) {
    return ProjectViewUtils.modifyTaskChildNode(myProject, childNode, myTask, this::createChildDirectoryNode);
  }

  public PsiDirectoryNode createChildDirectoryNode(PsiDirectory value) {
    return new DirectoryNode(myProject, value, getSettings());
  }

  @NotNull
  public Task getTask() {
    return myTask;
  }
}
