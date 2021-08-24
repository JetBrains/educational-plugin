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
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.problems.WolfTheProblemSolver;
import com.intellij.ui.EditorNotifications;
import com.intellij.util.ui.EmptyIcon;
import com.jetbrains.edu.EducationalCoreIcons;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder;
import com.jetbrains.edu.learning.courseFormat.CheckStatus;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.TaskFile;
import com.jetbrains.edu.learning.courseFormat.ext.TaskFileExt;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.messages.EduCoreBundle;
import com.jetbrains.edu.learning.placeholderDependencies.PlaceholderDependencyManager;
import com.jetbrains.edu.learning.projectView.ProgressUtil;
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector;
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionView;
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import static com.intellij.openapi.ui.Messages.*;
import static com.jetbrains.edu.learning.courseFormat.ext.TaskExt.revertTaskParameters;


public class RevertTaskAction extends DumbAwareAction implements RightAlignedToolbarAction {
  @NonNls
  public static final String ACTION_ID = "Educational.RefreshTask";
  private static final Logger LOG = Logger.getInstance(RevertTaskAction.class.getName());

  public RevertTaskAction() {
    super(EduCoreBundle.lazyMessage("action.reset.request"),
          EduCoreBundle.lazyMessage("action.reset.to.initial.state"),
          EducationalCoreIcons.ResetTask);
  }

  public static void revert(@NotNull final Project project) {
    final Task task = EduUtils.getCurrentTask(project);
    if (task == null) return;

    revertTaskFiles(task, project);
    revertTaskParameters(task, project);

    PlaceholderDependencyManager.updateDependentPlaceholders(project, task);
    EditorNotifications.getInstance(project).updateAllNotifications();
    Notification notification = new Notification("EduTools", EmptyIcon.ICON_16, "", "",
                                                 EduCoreBundle.message("action.reset.result"), NotificationType.INFORMATION, null);
    notification.notify(project);
    ProjectView.getInstance(project).refresh();
    TaskDescriptionView.getInstance(project).updateTaskSpecificPanel();
    TaskDescriptionView.getInstance(project).readyToCheck();
    ProgressUtil.updateCourseProgress(project);
  }

  private static void revertTaskFiles(@NotNull Task task, @NotNull Project project) {
    ApplicationManager.getApplication().runWriteAction(() -> {
      for (TaskFile taskFile : task.getTaskFiles().values()) {
        revertTaskFile(taskFile, project);
      }
    });
  }

  private static void revertTaskFile(@NotNull final TaskFile taskFile, @NotNull final Project project) {
    final Task task = taskFile.getTask();
    final Document document = TaskFileExt.getDocument(taskFile, project);
    final VirtualFile virtualFile = TaskFileExt.getVirtualFile(taskFile, project);
    // Note, nullable document is valid situation in case of binary files.
    if (document == null || virtualFile == null) {
      LOG.warn("Failed to find document and virtual file for task file " + taskFile.getName());
      return;
    }
    resetDocument(document, taskFile);
    task.setStatus(CheckStatus.Unchecked);
    resetAnswerPlaceholders(taskFile);

    WolfTheProblemSolver.getInstance(project).clearProblems(virtualFile);
    taskFile.setHighlightErrors(false);
    YamlFormatSynchronizer.saveItem(task);
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
    if (project == null) return;

    int result = showOkCancelDialog(project, EduCoreBundle.message("action.reset.progress.dropped"),
                                    EduCoreBundle.message("action.reset.request"), getOkButton(), getCancelButton(), getQuestionIcon());
    if (result != OK) return;
    revert(project);
    EduCounterUsageCollector.revertTask();
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
