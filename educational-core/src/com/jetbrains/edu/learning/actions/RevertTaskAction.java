package com.jetbrains.edu.learning.actions;

import com.intellij.ide.projectView.ProjectView;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.RightAlignedToolbarAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.problems.WolfTheProblemSolver;
import com.intellij.util.ui.EmptyIcon;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder;
import com.jetbrains.edu.learning.courseFormat.CheckStatus;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.TaskFile;
import com.jetbrains.edu.learning.courseFormat.ext.TaskFileExt;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.editor.EduEditor;
import com.jetbrains.edu.learning.placeholderDependencies.PlaceholderDependencyManager;
import com.jetbrains.edu.learning.statistics.EduUsagesCollector;
import com.jetbrains.edu.learning.ui.taskDescription.TaskDescriptionView;
import icons.EducationalCoreIcons;
import org.jetbrains.annotations.NotNull;


public class RevertTaskAction extends DumbAwareAction implements RightAlignedToolbarAction {
  public static final String ACTION_ID = "Educational.RefreshTask";
  private static final Logger LOG = Logger.getInstance(RevertTaskAction.class.getName());
  private static final String RESET_TASK = "Reset Task";

  public RevertTaskAction() {
    super(RESET_TASK, "Revert current task to the initial state", EducationalCoreIcons.ResetTask);
  }

  public static void revert(@NotNull final Project project) {
    final Task currentTask = EduUtils.getCurrentTask(project);
    if (currentTask == null) return;

    ApplicationManager.getApplication().runWriteAction(() -> {
      for (TaskFile taskFile : currentTask.getTaskFiles().values()) {
        revertTaskFile(taskFile, project);
      }
    });
    PlaceholderDependencyManager.updateDependentPlaceholders(project, currentTask);
    validateEditors(project);
    String message = String.format("<b>%s</b> task  of <b>%s</b> lesson is reset.", currentTask.getName(), currentTask.getLesson().getName());
    Notification notification = new Notification("reset.task", EmptyIcon.ICON_16, "", "", message, NotificationType.INFORMATION, null);
    notification.notify(project);
    ProjectView.getInstance(project).refresh();
    TaskDescriptionView.getInstance(project).updateTaskSpecificPanel();
    TaskDescriptionView.getInstance(project).readyToCheck();
    EduUtils.updateCourseProgress(project);
  }

  private static void validateEditors(@NotNull Project project) {
    final FileEditor[] editors = FileEditorManagerEx.getInstanceEx(project).getAllEditors();
    for (FileEditor editor : editors) {
      if (editor instanceof EduEditor) {
        ((EduEditor)editor).validateTaskFile();
      }
    }
  }

  private static void revertTaskFile(@NotNull final TaskFile taskFile, @NotNull final Project project) {
    final Task task = taskFile.getTask();
    final Document document = TaskFileExt.getDocument(taskFile, project);
    final VirtualFile virtualFile = TaskFileExt.getVirtualFile(taskFile, project);
    if (document == null || virtualFile == null) {
      LOG.warn("Failed to find document and virtual file for task file " + taskFile.getName());
      return;
    }
    resetDocument(document, taskFile);
    task.setStatus(CheckStatus.Unchecked);
    resetAnswerPlaceholders(taskFile);

    WolfTheProblemSolver.getInstance(project).clearProblems(virtualFile);
    taskFile.setHighlightErrors(false);
  }

  static void resetAnswerPlaceholders(@NotNull TaskFile selectedTaskFile) {
    for (AnswerPlaceholder answerPlaceholder : selectedTaskFile.getAnswerPlaceholders()) {
      answerPlaceholder.reset(true);
    }
  }

  public static void resetDocument(@NotNull final Document document, @NotNull final TaskFile taskFile) {
    taskFile.setTrackChanges(false);
    document.setText(taskFile.getText());
    taskFile.setTrackChanges(true);
  }

  public void actionPerformed(@NotNull AnActionEvent event) {
    final Project project = event.getProject();
    if (project == null) {
      return;
    }
    revert(project);
    EduUsagesCollector.taskReverted();
  }

  @Override
  public void update(@NotNull AnActionEvent event) {
    EduUtils.updateAction(event);
    final Project project = event.getProject();
    if (project == null) {
      return;
    }
    Course course = StudyTaskManager.getInstance(project).getCourse();
    if (course == null) {
      return;
    }

    Presentation presentation = event.getPresentation();

    Task task = EduUtils.getCurrentTask(project);
    if (task == null) {
      return;
    }
    if (!course.isStudy()) {
      presentation.setEnabledAndVisible(false);
    }
  }
}
