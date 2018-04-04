package com.jetbrains.edu.coursecreator.handlers;

import com.intellij.ide.IdeView;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.refactoring.move.MoveCallback;
import com.intellij.refactoring.move.MoveHandlerDelegate;
import com.jetbrains.edu.coursecreator.CCUtils;
import com.jetbrains.edu.coursecreator.ui.CCMoveStudyItemDialog;
import com.jetbrains.edu.learning.EduNames;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.Section;
import com.jetbrains.edu.learning.courseFormat.StudyItem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CCSectionMoveHandlerDelegate extends MoveHandlerDelegate {

  @Override
  public boolean canMove(DataContext dataContext) {
    if (CommonDataKeys.PSI_FILE.getData(dataContext) != null) {
      return false;
    }
    IdeView view = LangDataKeys.IDE_VIEW.getData(dataContext);
    if (view == null) {
      return false;
    }
    return canMove(view.getDirectories(), null);
  }

  @Override
  public boolean canMove(PsiElement[] elements, @Nullable PsiElement targetContainer) {
    if (elements.length != 1 || !(elements[0] instanceof PsiDirectory)) {
      return false;
    }
    PsiDirectory element = (PsiDirectory)elements[0];
    Course course = StudyTaskManager.getInstance(element.getProject()).getCourse();
    if (course == null) {
      return false;
    }
    return course.getSection(element.getName()) != null;
  }

  @Override
  public boolean isValidTarget(PsiElement psiElement, PsiElement[] sources) {
    return true;
  }

  @Override
  public void doMove(final Project project,
                     PsiElement[] elements,
                     @Nullable PsiElement targetDirectory,
                     @Nullable MoveCallback callback) {
    if (!(targetDirectory instanceof PsiDirectory)) {
      return;
    }
    final Course course = StudyTaskManager.getInstance(project).getCourse();
    if (course == null) {
      return;
    }
    final PsiDirectory sourceDirectory = (PsiDirectory)elements[0];
    final Section sourceSection = course.getSection(sourceDirectory.getName());
    if (sourceSection == null) {
      Messages.showInfoMessage("Can't find source section to move", "Incorrect Source For Move");
      return;
    }

    StudyItem targetItem = course.getItem(((PsiDirectory)targetDirectory).getName());
    if (targetItem == null) {
      Messages.showInfoMessage("Sections can be moved only to top level", "Incorrect Target For Move");
      return;
    }

    final int delta = getDelta(project, targetItem);

    int sourceSectionIndex = sourceSection.getIndex();
    sourceSection.setIndex(-1);

    final VirtualFile[] itemDirs = EduUtils.getCourseDir(project).getChildren();
    CCUtils.updateHigherElements(itemDirs, file -> course.getItem(file.getName()), sourceSectionIndex, -1);

    final int newItemIndex = targetItem.getIndex() + delta;
    CCUtils.updateHigherElements(itemDirs, file -> course.getItem(file.getName()), newItemIndex - 1, 1);

    sourceSection.setIndex(newItemIndex);

    course.sortItems();
    ProjectView.getInstance(project).refresh();
  }

  protected int getDelta(@NotNull Project project, @NotNull StudyItem targetItem) {
    final CCMoveStudyItemDialog dialog = new CCMoveStudyItemDialog(project, EduNames.SECTION, targetItem.getName());
    dialog.show();
    if (dialog.getExitCode() != DialogWrapper.OK_EXIT_CODE) {
      return -1;
    }
    return dialog.getIndexDelta();
  }

  @Override
  public boolean tryToMove(PsiElement element,
                           Project project,
                           DataContext dataContext,
                           @Nullable PsiReference reference,
                           Editor editor) {
    return CCUtils.isCourseCreator(project);
  }
}
