package com.jetbrains.edu.learning.actions;

import com.intellij.ide.projectView.ProjectView;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.RightAlignedToolbarAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.BalloonBuilder;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.problems.WolfTheProblemSolver;
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
import com.jetbrains.edu.learning.ui.taskDescription.TaskDescriptionView;
import icons.EducationalCoreIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class RevertTaskAction extends DumbAwareActionWithShortcut implements RightAlignedToolbarAction {
  public static final String ACTION_ID = "Educational.RefreshTask";
  public static final String SHORTCUT = "ctrl shift pressed X";
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
    showBalloon(project);
    ProjectView.getInstance(project).refresh();
    EduUtils.updateToolWindows(project);
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

    TaskDescriptionView.getInstance(project).updateTaskSpecificPanel();

    WolfTheProblemSolver.getInstance(project).clearProblems(virtualFile);
    taskFile.setHighlightErrors(false);
  }

  static void resetAnswerPlaceholders(@NotNull TaskFile selectedTaskFile) {
    for (AnswerPlaceholder answerPlaceholder : selectedTaskFile.getAnswerPlaceholders()) {
      answerPlaceholder.reset();
    }
  }

  public static void resetDocument(@NotNull final Document document, @NotNull final TaskFile taskFile) {
    taskFile.setTrackChanges(false);
    document.setText(taskFile.getText());
    taskFile.setTrackChanges(true);
  }

  private static void showBalloon(@NotNull final Project project) {
    BalloonBuilder balloonBuilder =
      JBPopupFactory.getInstance().createHtmlTextBalloonBuilder("You can start again now", MessageType.INFO, null);
    final Balloon balloon = balloonBuilder.createBalloon();
    EduEditor selectedEduEditor = EduUtils.getSelectedEduEditor(project);
    assert selectedEduEditor != null;
    balloon.show(EduUtils.computeLocation(selectedEduEditor.getEditor()), Balloon.Position.above);
    Disposer.register(project, balloon);
  }

  public void actionPerformed(@NotNull AnActionEvent event) {
    final Project project = event.getProject();
    if (project == null) {
      return;
    }
    revert(project);
  }

  @Override
  public void update(AnActionEvent event) {
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
      presentation.setVisible(true);
      presentation.setEnabled(false);
    }
  }

  @NotNull
  @Override
  public String getActionId() {
    return ACTION_ID;
  }

  @Nullable
  @Override
  public String[] getShortcuts() {
    return new String[]{SHORTCUT};
  }
}
