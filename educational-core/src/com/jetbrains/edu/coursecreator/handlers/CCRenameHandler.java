package com.jetbrains.edu.coursecreator.handlers;

import com.intellij.ide.projectView.ProjectView;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileSystemItem;
import com.jetbrains.edu.coursecreator.CCUtils;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.configuration.EduConfigurator;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.StudyItem;
import com.jetbrains.edu.learning.courseFormat.ext.CourseExt;
import com.jetbrains.edu.learning.handlers.EduRenameHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

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


  protected void processRename(@NotNull final StudyItem item,
                               @NotNull String namePrefix,
                               @NotNull Course course,
                               @NotNull final Project project,
                               @NotNull VirtualFile directory) {
    final EduConfigurator<?> configurator = CourseExt.getConfigurator(course);

    String name = item.getName();
    String text = "Rename " + StringUtil.toTitleCase(namePrefix);
    String newName = Messages
      .showInputDialog(project, text + " '" + name + "' to", text, null, name,
                       new ItemNameValidator(directory.getParent(), name, configurator));
    if (newName != null) {
      item.setName(newName);
      ApplicationManager.getApplication().runWriteAction(() -> {
        try {
          directory.rename(CCRenameHandler.class, newName);
        }
        catch (IOException e) {
          Logger.getInstance(CCRenameHandler.class).error(e);
        }
      });
      if (configurator != null) {
        configurator.getCourseBuilder().refreshProject(project);
      }
    }
  }

  @Nullable
  protected String performCustomNameValidation(@NotNull String name) {
    return null;
  }

  private class ItemNameValidator extends CCUtils.PathInputValidator {
    private final EduConfigurator<?> configurator;

    public ItemNameValidator(@Nullable VirtualFile myParentDir, @Nullable String myName, @Nullable EduConfigurator<?> configurator) {
      super(myParentDir, myName);
      this.configurator = configurator;
    }

    @Override
    public boolean checkInput(@NotNull String inputString) {
      if (super.checkInput(inputString)) {
        if (configurator != null) {
          setMyErrorText(configurator.getCustomItemNameValidator().apply(inputString));
        }
        if (getMyErrorText() == null) {
          setMyErrorText(performCustomNameValidation(inputString));
        }
      }
      return getMyErrorText() == null;
    }
  }

  @Override
  public void invoke(@NotNull Project project, @NotNull PsiElement[] elements, DataContext dataContext) {
    invoke(project, null, null, dataContext);
  }
}
