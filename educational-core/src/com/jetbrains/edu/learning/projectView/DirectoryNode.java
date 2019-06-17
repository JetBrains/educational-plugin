package com.jetbrains.edu.learning.projectView;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.ext.CourseExt;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DirectoryNode extends EduNode {

  @NotNull
  protected final Project myProject;
  protected final ViewSettings myViewSettings;
  @Nullable
  protected final Task myTask;

  public DirectoryNode(@NotNull Project project,
                       PsiDirectory value,
                       ViewSettings viewSettings,
                       @Nullable Task task) {
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
  public AbstractTreeNode modifyChildNode(@NotNull AbstractTreeNode childNode) {
    return CourseViewUtils.modifyTaskChildNode(myProject, childNode, myTask, this::createChildDirectoryNode);
  }

  public PsiDirectoryNode createChildDirectoryNode(PsiDirectory value) {
    return new DirectoryNode(myProject, value, myViewSettings, myTask);
  }

  @Override
  protected void updateImpl(PresentationData data) {
    Project project = getProject();
    if (project != null) {
      PsiDirectory dir = getValue();
      VirtualFile directoryFile = dir.getVirtualFile();
      String name = directoryFile.getName();

      Course course = StudyTaskManager.getInstance(myProject).getCourse();
      if (course == null) {
        return;
      }

      if (name.equals(CourseExt.getSourceDir(course)) || CourseExt.getTestDirs(course).contains(name)) {
        data.setPresentableText(name);
        return;
      }
    }

    super.updateImpl(data);
  }
}
