package com.jetbrains.edu.learning.actions;

import com.intellij.ide.projectView.ProjectView;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.RightAlignedToolbarAction;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.ui.EditorNotifications;
import com.intellij.util.ui.EmptyIcon;
import com.jetbrains.edu.EducationalCoreIcons;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseFormat.Course;
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
import static com.jetbrains.edu.learning.courseFormat.ext.TaskExt.revertTaskFiles;
import static com.jetbrains.edu.learning.courseFormat.ext.TaskExt.revertTaskParameters;


public class RevertTaskAction extends DumbAwareAction implements RightAlignedToolbarAction {
  @NonNls
  public static final String ACTION_ID = "Educational.RefreshTask";

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
    YamlFormatSynchronizer.saveItem(task);

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
