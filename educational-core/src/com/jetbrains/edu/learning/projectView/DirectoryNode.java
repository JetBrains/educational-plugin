package com.jetbrains.edu.learning.projectView;

import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DirectoryNode extends EduNode {

  @NotNull
  protected final Project myProject;
  protected final ViewSettings myViewSettings;
  @NotNull
  protected final Task myTask;

  public DirectoryNode(@NotNull Project project,
                       PsiDirectory value,
                       ViewSettings viewSettings,
                       @NotNull Task task) {
    super(project, value, viewSettings);
    myProject = project;
    myViewSettings = viewSettings;
    myTask = task;
  }

  @Override
  public boolean canNavigate() {
    return true;
  }

  @Nullable
  @Override
  public AbstractTreeNode modifyChildNode(AbstractTreeNode childNode) {
    return CourseViewUtils.modifyTaskChildNode(myProject, childNode, myTask, this::createChildDirectoryNode);
  }

  public PsiDirectoryNode createChildDirectoryNode(PsiDirectory value) {
    return new DirectoryNode(myProject, value, myViewSettings, myTask);
  }
}
