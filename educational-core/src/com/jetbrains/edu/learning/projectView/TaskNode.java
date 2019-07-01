package com.jetbrains.edu.learning.projectView;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.navigation.NavigationUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
  public void updateImpl(@NotNull PresentationData data) {
    updatePresentation(myTask, data);
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
  public AbstractTreeNode modifyChildNode(@NotNull AbstractTreeNode childNode) {
    return CourseViewUtils.modifyTaskChildNode(myProject, childNode, myTask, this::createChildDirectoryNode);
  }

  public PsiDirectoryNode createChildDirectoryNode(PsiDirectory value) {
    return new DirectoryNode(myProject, value, getSettings(), myTask);
  }

  @NotNull
  public Task getTask() {
    return myTask;
  }
}
