package com.jetbrains.edu.coursecreator.handlers;

import com.intellij.ide.IdeView;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
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
import com.jetbrains.edu.learning.courseFormat.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public class CCLessonMoveHandlerDelegate extends MoveHandlerDelegate {

  private static final Logger LOG = Logger.getInstance(CCLessonMoveHandlerDelegate.class);

  @Override
  public boolean canMove(DataContext dataContext) {
    if (CommonDataKeys.PSI_FILE.getData(dataContext) != null) {
      return false;
    }
    IdeView view = LangDataKeys.IDE_VIEW.getData(dataContext);
    if (view == null) {
      return false;
    }

    final PsiDirectory[] directories = view.getDirectories();
    if (directories.length == 0 || directories.length > 1) {
      return false;
    }

    final PsiDirectory sourceDirectory = directories[0];
    return EduUtils.isLessonDirectory(sourceDirectory.getProject(), sourceDirectory.getVirtualFile());
  }

  @Override
  public boolean canMove(PsiElement[] elements, @Nullable PsiElement targetContainer) {
    if (elements.length > 0 && elements[0] instanceof PsiDirectory) {
      PsiDirectory element = (PsiDirectory)elements[0];
      return EduUtils.isLessonDirectory(element.getProject(), element.getVirtualFile());
    }
    return false;
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
    final VirtualFile sourceVFile = sourceDirectory.getVirtualFile();
    final Lesson sourceLesson = EduUtils.getLesson(sourceVFile, course);
    if (sourceLesson == null) {
      Messages.showInfoMessage("Can't find source lesson to move", "Incorrect Source For Move");
      return;
    }

    final VirtualFile targetVFile = ((PsiDirectory)targetDirectory).getVirtualFile();
    StudyItem targetItem = getTargetItem(course, targetVFile, project);
    if (targetItem == null) {
      Messages.showInfoMessage("Lessons can be moved only to other lessons or sections", "Incorrect Target For Move");
      return;
    }
    int delta = 0;
    VirtualFile sourceParentDir = sourceVFile.getParent();
    VirtualFile targetParentDir;
    if (targetItem instanceof LessonContainer) {
      targetParentDir = targetVFile;
    }
    else {
      final CCMoveStudyItemDialog dialog = new CCMoveStudyItemDialog(project, EduNames.LESSON, targetItem.getName());
      dialog.show();
      if (dialog.getExitCode() != DialogWrapper.OK_EXIT_CODE) {
        return;
      }
      targetParentDir = targetVFile.getParent();
      delta = dialog.getIndexDelta();
    }
    final Section targetSection = course.getSection(targetParentDir.getName());
    final LessonContainer targetContainer = targetSection != null ? targetSection : course;

    final LessonContainer sourceContainer = sourceLesson.getContainer();

    int sourceLessonIndex = sourceLesson.getIndex();
    sourceLesson.setIndex(-1);
    CCUtils.updateHigherElements(sourceParentDir.getChildren(), file -> sourceContainer.getChild(file.getName()), sourceLessonIndex, -1);

    final int newItemIndex = targetItem instanceof LessonContainer ? 1 : targetItem.getIndex() + delta;
    CCUtils.updateHigherElements(targetParentDir.getChildren(), file -> targetContainer.getChild(file.getName()), newItemIndex - 1, 1);

    sourceLesson.setIndex(newItemIndex);
    sourceLesson.setSection(targetSection);

    sourceContainer.removeLesson(sourceLesson);
    targetContainer.addLesson(sourceLesson);

    course.sortChildren();
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {
        try {
          if (!targetParentDir.equals(sourceVFile.getParent())) {
            sourceVFile.move(this, targetParentDir);
          }
        }
        catch (IOException e) {
          LOG.error(e);
        }
      }
    });
    ProjectView.getInstance(project).refresh();
  }

  private static StudyItem getTargetItem(@NotNull Course course, @NotNull VirtualFile targetVFile, @NotNull Project project) {
    if (targetVFile.equals(project.getBaseDir())) return course;
    StudyItem targetItem = course.getItem(targetVFile.getName());
    if (targetItem == null) {
      targetItem = EduUtils.getLesson(targetVFile, course);
    }
    return targetItem;
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
