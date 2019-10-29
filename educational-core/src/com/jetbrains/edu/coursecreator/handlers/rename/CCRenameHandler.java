package com.jetbrains.edu.coursecreator.handlers.rename;

import com.intellij.ide.projectView.ProjectView;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileSystemItem;
import com.jetbrains.edu.coursecreator.CCUtils;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.handlers.EduRenameHandler;
import org.jetbrains.annotations.NotNull;

public abstract class CCRenameHandler implements EduRenameHandler {

  @Override
  public boolean isAvailableOnDataContext(@NotNull DataContext dataContext) {
    PsiElement element = CommonDataKeys.PSI_ELEMENT.getData(dataContext);
    if (!(element instanceof PsiFileSystemItem)) {
      return false;
    }
    if (!CCUtils.isCourseCreator(element.getProject())) {
      return false;
    }
    VirtualFile file = ((PsiFileSystemItem)element).getVirtualFile();
    return isAvailable(element.getProject(), file);
  }

  protected abstract boolean isAvailable(@NotNull Project project, @NotNull VirtualFile file);

  @Override
  public boolean isRenaming(@NotNull DataContext dataContext) {
    return isAvailableOnDataContext(dataContext);
  }

  @Override
  public void invoke(@NotNull Project project, Editor editor, PsiFile file, DataContext dataContext) {
    PsiElement element = CommonDataKeys.PSI_ELEMENT.getData(dataContext);
    assert element != null;
    PsiFileSystemItem psiFileSystemItem = (PsiFileSystemItem)element;
    Course course = StudyTaskManager.getInstance(project).getCourse();
    if (course == null) {
      return;
    }
    rename(project, course, psiFileSystemItem);
    ProjectView.getInstance(project).refresh();
    FileEditorManagerEx managerEx = FileEditorManagerEx.getInstanceEx(project);
    for (VirtualFile virtualFile : managerEx.getOpenFiles()) {
      managerEx.updateFilePresentation(virtualFile);
    }
  }

  protected abstract void rename(@NotNull Project project, @NotNull Course course, @NotNull PsiFileSystemItem item);

  @Override
  public void invoke(@NotNull Project project, @NotNull PsiElement[] elements, DataContext dataContext) {
    invoke(project, null, null, dataContext);
  }
}
