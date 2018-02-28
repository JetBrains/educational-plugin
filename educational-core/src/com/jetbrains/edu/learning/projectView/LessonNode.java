package com.jetbrains.edu.learning.projectView;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiManager;
import com.intellij.ui.JBColor;
import com.jetbrains.edu.coursecreator.CCUtils;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.courseFormat.Lesson;
import com.jetbrains.edu.learning.courseFormat.StudyItem;
import com.jetbrains.edu.learning.courseFormat.CheckStatus;
import com.jetbrains.edu.learning.courseFormat.ext.TaskExt;
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
    boolean isSolved = status != CheckStatus.Solved;
    JBColor color = isSolved ? JBColor.BLACK : LIGHT_GREEN;
    Icon icon = isSolved ? EducationalCoreIcons.Lesson : EducationalCoreIcons.LessonSolved;
    updatePresentation(data, myLesson.getName(), color, icon, null);
  }

  @Override
  public int getWeight() {
    return myLesson.getIndex();
  }

  @Nullable
  @Override
  public AbstractTreeNode modifyChildNode(AbstractTreeNode childNode) {
    Object value = childNode.getValue();
    if (value instanceof PsiDirectory) {
      PsiDirectory directory = (PsiDirectory)value;
      Task task = myLesson.getTask(directory.getName());
      if (task == null) {
        return null;
      }
      String sourceDir = TaskExt.getSourceDir(task);
      if (StringUtil.isNotEmpty(sourceDir)) {
        VirtualFile srcDir = directory.getVirtualFile().findChild(sourceDir);
        boolean isCourseCreatorGradleProject = EduUtils.isConfiguredWithGradle(myProject) && CCUtils.isCourseCreator(myProject);
        if (srcDir != null && !isCourseCreatorGradleProject) {
          directory = PsiManager.getInstance(myProject).findDirectory(srcDir);
          if (directory == null) {
            return null;
          }
        }
      }
      return createChildDirectoryNode(task, directory);
    }
    return null;
  }

  @Override
  public PsiDirectoryNode createChildDirectoryNode(StudyItem item, PsiDirectory directory) {
    return new TaskNode(myProject, directory, myViewSettings, ((Task)item));
  }
}
