package com.jetbrains.edu.learning.projectView;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import com.intellij.ui.JBColor;
import com.jetbrains.edu.learning.courseFormat.CheckStatus;
import com.jetbrains.edu.learning.courseFormat.Lesson;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import icons.EducationalCoreIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class LessonNode extends EduNode {
  @NotNull protected final Project myProject;
  protected final ViewSettings myViewSettings;
  @NotNull protected final Lesson myLesson;

  public LessonNode(@NotNull Project project,
                    PsiDirectory value,
                    ViewSettings viewSettings,
                    @NotNull Lesson lesson) {
    super(project, value, viewSettings);
    myProject = project;
    myViewSettings = viewSettings;
    myLesson = lesson;
  }

  @Override
  protected void updateImpl(PresentationData data) {
    CheckStatus status = myLesson.getStatus();
    boolean isSolved = status == CheckStatus.Solved;
    JBColor color = !isSolved ? JBColor.BLACK : LIGHT_GREEN;
    Icon icon = !isSolved ? EducationalCoreIcons.Lesson : EducationalCoreIcons.LessonSolved;
    updatePresentation(data, myLesson.getPresentableName(), color, icon, null);
  }

  @Override
  public int getWeight() {
    return myLesson.getIndex();
  }

  @Nullable
  @Override
  protected AbstractTreeNode modifyChildNode(AbstractTreeNode child) {
    Object value = child.getValue();
    if (value instanceof PsiDirectory) {
      PsiDirectory directory = (PsiDirectory)value;
      Task task = myLesson.getTask(directory.getName());
      if (task == null) {
        return null;
      }
      PsiDirectory taskDirectory = ProjectViewUtils.findTaskDirectory(myProject, directory, task);
      if (taskDirectory == null) return null;
      return createTaskNode(taskDirectory, task);
    }
    return null;
  }

  @NotNull
  protected TaskNode createTaskNode(PsiDirectory directory, Task task) {
    return new TaskNode(myProject, directory, myViewSettings, task);
  }

  @NotNull
  public Lesson getLesson() {
    return myLesson;
  }
}
