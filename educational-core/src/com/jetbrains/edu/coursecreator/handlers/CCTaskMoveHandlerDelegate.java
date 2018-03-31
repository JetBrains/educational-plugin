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
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.Lesson;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class CCTaskMoveHandlerDelegate extends MoveHandlerDelegate {

  private static final Logger LOG = Logger.getInstance(CCTaskMoveHandlerDelegate.class);
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
    return EduUtils.isTaskDirectory(sourceDirectory.getProject(), sourceDirectory.getVirtualFile());
  }

  @Override
  public boolean canMove(PsiElement[] elements, @Nullable PsiElement targetContainer) {
    if (elements.length > 0 && elements[0] instanceof PsiDirectory) {
      PsiDirectory element = (PsiDirectory)elements[0];
      return EduUtils.isTaskDirectory(element.getProject(), element.getVirtualFile());
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
                     @Nullable PsiElement targetContainer,
                     @Nullable MoveCallback callback) {
    if (!(targetContainer instanceof PsiDirectory)) {
      return;
    }

    final VirtualFile targetVFile = ((PsiDirectory)targetContainer).getVirtualFile();

    if (!EduUtils.isTaskDirectory(project, targetVFile) && !EduUtils.isLessonDirectory(project, targetVFile)) {
      Messages.showInfoMessage("Tasks can be moved only to other lessons or inside lesson", "Incorrect Target For Move");
      return;
    }
    final Course course = StudyTaskManager.getInstance(project).getCourse();
    final PsiDirectory sourceDirectory = (PsiDirectory)elements[0];

    if (course == null) {
      return;
    }
    final Task taskToMove = EduUtils.getTask(sourceDirectory.getVirtualFile(), course);
    if (taskToMove == null) {
      return;
    }

    if (EduUtils.isLessonDirectory(project, targetVFile)) {
      //if user moves task to any lesson, this task is inserted as the last task in this lesson
      Lesson targetLesson = EduUtils.getLesson(targetVFile, course);
      if (targetLesson == null) {
        return;
      }
      if (targetVFile.findChild(taskToMove.getName()) != null) {
        Messages.showInfoMessage("Lesson contains task with the same name", "Incorrect Target For Move");
        return;
      }
      List<Task> taskList = targetLesson.getTaskList();
      moveTask(sourceDirectory, taskToMove, taskList.isEmpty() ? null : taskList.get(taskList.size() - 1),
               1, targetVFile, targetLesson);
    }
    else {
      VirtualFile lessonDir = targetVFile.getParent();
      if (lessonDir == null) {
        return;
      }
      Task targetTask = EduUtils.getTask(targetVFile, course);
      if (targetTask == null) {
        return;
      }
      final CCMoveStudyItemDialog dialog = new CCMoveStudyItemDialog(project, EduNames.TASK, targetTask.getName());
      dialog.show();
      if (dialog.getExitCode() != DialogWrapper.OK_EXIT_CODE) {
        return;
      }
      moveTask(sourceDirectory, taskToMove, targetTask, dialog.getIndexDelta(), lessonDir, targetTask.getLesson());
    }
    ProjectView.getInstance(project).refresh();

  }

  private void moveTask(final PsiDirectory sourceDirectory,
                        final Task taskToMove,
                        Task targetTask,
                        int indexDelta,
                        final VirtualFile targetDirectory,
                        Lesson targetLesson) {
    final VirtualFile sourceLessonDir = sourceDirectory.getVirtualFile().getParent();
    if (sourceLessonDir == null) {
      return;
    }
    CCUtils.updateHigherElements(sourceLessonDir.getChildren(), file -> taskToMove.getLesson().getTask(file.getName()),
                                 taskToMove.getIndex(),-1);

    final int newItemIndex = targetTask != null ? targetTask.getIndex() + indexDelta : 1;
    taskToMove.setIndex(-1);
    taskToMove.getLesson().getTaskList().remove(taskToMove);
    final Lesson finalTargetLesson = targetLesson;
    CCUtils.updateHigherElements(targetDirectory.getChildren(), file -> finalTargetLesson.getTask(file.getName()), newItemIndex - 1, 1);

    taskToMove.setIndex(newItemIndex);
    taskToMove.setLesson(targetLesson);
    targetLesson.getTaskList().add(taskToMove);
    Collections.sort(targetLesson.getTaskList(), EduUtils.INDEX_COMPARATOR);
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {
        try {
          //moving file to the same directory leads to exception
          if (!targetDirectory.equals(sourceLessonDir)) {
            sourceDirectory.getVirtualFile().move(this, targetDirectory);
          }
        }
        catch (IOException e) {
          LOG.error(e);
        }
      }
    });
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
