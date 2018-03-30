package com.jetbrains.edu.java;

import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.refactoring.rename.RenameHandler;
import com.jetbrains.edu.learning.handlers.EduMoveDelegate;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.courseFormat.Course;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JMoveRenameHandler extends EduMoveDelegate implements RenameHandler {
  @Override
  public boolean canMove(DataContext dataContext) {
    return canRenameOrMove(CommonDataKeys.PROJECT.getData(dataContext), CommonDataKeys.PSI_ELEMENT.getData(dataContext));
  }

  @Override
  public boolean canMove(PsiElement[] elements, @Nullable PsiElement targetContainer) {
    if (elements.length == 1) {
      return canRenameOrMove(elements[0].getProject(), elements[0]);
    }
    return false;
  }

  @Override
  public boolean isAvailableOnDataContext(DataContext dataContext) {
    return canRenameOrMove(CommonDataKeys.PROJECT.getData(dataContext), CommonDataKeys.PSI_ELEMENT.getData(dataContext));
  }

  @Override
  public boolean isRenaming(DataContext dataContext) {
    return isAvailableOnDataContext(dataContext);
  }

  @Override
  public void invoke(@NotNull Project project, Editor editor, PsiFile psiFile, DataContext dataContext) {
    Messages.showInfoMessage("This rename operation can break the course", "Invalid Rename Operation");
  }

  @Override
  public void invoke(@NotNull Project project, @NotNull PsiElement[] psiElements, DataContext dataContext) {
    invoke(project, null, null, dataContext);
  }

  private static boolean canRenameOrMove(@Nullable Project project, @Nullable PsiElement element) {
    if (element == null || project == null) {
      return false;
    }
    Course course = StudyTaskManager.getInstance(project).getCourse();
    if (!EduUtils.isStudentProject(project)) {
      return false;
    }

    assert course != null;
    PsiElement elementToMove = getElementToMove(element, course);
    return !EduUtils.isRenameAndMoveForbidden(project, course, elementToMove);
  }

  private static PsiElement getElementToMove(@NotNull PsiElement element, @NotNull Course course) {
    // prevent class renaming in adaptive courses
    if (course.isAdaptive() && element instanceof PsiClass) {
      String fileName = element.getContainingFile().getName();
      int dotIndex = fileName.lastIndexOf('.');
      String fileNameWithoutExtension = dotIndex >= 0 ? fileName.substring(0, dotIndex) : fileName;
      String className = ((PsiClass) element).getName();
      if (fileNameWithoutExtension.equals(className)) {
        return element.getContainingFile();
      }
    }

    return element;
  }
}
