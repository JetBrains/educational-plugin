package com.jetbrains.edu.coursecreator.projectView;

import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiNamedElement;
import com.jetbrains.edu.learning.*;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.ext.TaskExt;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.projectView.TaskNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CCTaskNode extends TaskNode {
  public CCTaskNode(@NotNull Project project,
                    PsiDirectory value,
                    ViewSettings viewSettings,
                    @NotNull Task task) {
    super(project, value, viewSettings, task);
  }

  @Nullable
  @Override
  public AbstractTreeNode modifyChildNode(AbstractTreeNode childNode) {
    AbstractTreeNode node = super.modifyChildNode(childNode);
    if (node != null) {
      return node;
    }
    Object value = childNode.getValue();
    if (value instanceof PsiDirectory) {
      String name = ((PsiDirectory) value).getName();
      if (EduNames.BUILD.equals(name) || EduNames.OUT.equals(name)) {
        return null;
      }

      if (name.equals(TaskExt.getSourceDir(myTask)) || name.equals(TaskExt.getTestDir(myTask))) {
        return createChildDirectoryNode((PsiDirectory) value);
      }
    }
    if (value instanceof PsiElement) {
      PsiElement psiElement = (PsiElement) value;
      PsiFile psiFile = psiElement.getContainingFile();
      VirtualFile virtualFile = psiFile.getVirtualFile();
      if (virtualFile == null) {
        return null;
      }
      Course course = StudyTaskManager.getInstance(myProject).getCourse();
      if (course == null) {
        return null;
      }
      EduConfigurator configurator = EduConfiguratorManager.forLanguage(course.getLanguageById());
      if (configurator == null) {
        return new CCStudentInvisibleFileNode(myProject, psiFile, getSettings());
      }
      if (!EduUtils.isTestsFile(myProject, virtualFile)) {
        return new CCStudentInvisibleFileNode(myProject, psiFile, getSettings());
      }
      return new CCStudentInvisibleFileNode(myProject, psiFile, getSettings(), getTestNodeName(configurator, psiElement));
    }
    return null;
  }

  @NotNull
  private static String getTestNodeName(EduConfigurator configurator, PsiElement psiElement) {
    String defaultTestName = configurator.getTestFileName();
    if (psiElement instanceof PsiFile) {
      return defaultTestName;
    }
    if (psiElement instanceof PsiNamedElement) {
      String name = ((PsiNamedElement)psiElement).getName();
      return name != null ? name : defaultTestName;
    }
    return defaultTestName;
  }

  @Override
  public PsiDirectoryNode createChildDirectoryNode(PsiDirectory value) {
    return new CCNode(myProject, value, getSettings());
  }
}
