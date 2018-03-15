package com.jetbrains.edu.learning.actions;

import com.intellij.icons.AllIcons;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.BalloonBuilder;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.problems.WolfTheProblemSolver;
import com.jetbrains.edu.learning.EduState;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder;
import com.jetbrains.edu.learning.courseFormat.CheckStatus;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.TaskFile;
import com.jetbrains.edu.learning.courseFormat.tasks.ChoiceTask;
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.editor.ChoiceVariantsPanel;
import com.jetbrains.edu.learning.editor.EduEditor;
import com.jetbrains.edu.learning.navigation.NavigationUtils;
import com.jetbrains.edu.learning.ui.taskDescription.TaskDescriptionToolWindow;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public class RefreshTaskFileAction extends DumbAwareActionWithShortcut {
  public static final String ACTION_ID = "Educational.RefreshTaskFile";
  public static final String SHORTCUT = "ctrl shift pressed X";
  private static final Logger LOG = Logger.getInstance(RefreshTaskFileAction.class.getName());
  private static final String TEXT = "Reset Task File";
  private static final String RESET_TASK = "Reset Task";

  public RefreshTaskFileAction() {
    super(TEXT, "Refresh current task", AllIcons.Actions.Rollback);
  }

  public static void refresh(@NotNull final Project project) {
    ApplicationManager.getApplication().runWriteAction(() -> {
      EduEditor eduEditor = EduUtils.getSelectedEduEditor(project);
      EduState eduState = new EduState(eduEditor);
      if (eduEditor == null || !eduState.isValid()) {
        LOG.info("RefreshTaskFileAction was invoked outside of Study Editor");
        return;
      }
      refreshFile(eduState, project);
      eduEditor.validateTaskFile();
    });
  }

  private static void refreshFile(@NotNull final EduState eduState, @NotNull final Project project) {
    final Editor editor = eduState.getEditor();
    final TaskFile taskFile = eduState.getTaskFile();
    if (taskFile == null || editor == null) return;
    final Task task = taskFile.getTask();
    if (!resetTaskFile(editor.getDocument(), project, taskFile)) {
      Messages.showInfoMessage("The initial text of task file is unavailable", "Failed to Refresh Task File");
      return;
    }
    if (task instanceof ChoiceTask) {
      final TaskDescriptionToolWindow window = EduUtils.getStudyToolWindow(project);
      if (window != null) {
        window.setBottomComponent(new ChoiceVariantsPanel((ChoiceTask)task));
      }
    }

    WolfTheProblemSolver.getInstance(project).clearProblems(eduState.getVirtualFile());
    taskFile.setHighlightErrors(false);
    ApplicationManager.getApplication().invokeLater(
      () -> IdeFocusManager.getInstance(project).requestFocus(editor.getContentComponent(), true));

    NavigationUtils.navigateToFirstAnswerPlaceholder(editor, taskFile);
    showBalloon(project, MessageType.INFO);
  }

  private static boolean resetTaskFile(@NotNull final Document document,
                                       @NotNull final Project project,
                                       TaskFile taskFile) {
    resetDocument(document, taskFile);
    final Task task = taskFile.getTask();
    task.setStatus(CheckStatus.Unchecked);
    if (task instanceof ChoiceTask) {
      ((ChoiceTask)task).setSelectedVariants(new ArrayList<>());
    }
    resetAnswerPlaceholders(taskFile, project);
    ProjectView.getInstance(project).refresh();
    EduUtils.updateToolWindows(project);
    return true;
  }

  private static void showBalloon(@NotNull final Project project, @NotNull final MessageType messageType) {
    BalloonBuilder balloonBuilder =
      JBPopupFactory.getInstance().createHtmlTextBalloonBuilder("You can start again now", messageType, null);
    final Balloon balloon = balloonBuilder.createBalloon();
    EduEditor selectedEduEditor = EduUtils.getSelectedEduEditor(project);
    assert selectedEduEditor != null;
    balloon.show(EduUtils.computeLocation(selectedEduEditor.getEditor()), Balloon.Position.above);
    Disposer.register(project, balloon);
  }

  static void resetAnswerPlaceholders(TaskFile selectedTaskFile, Project project) {
    final StudyTaskManager studyTaskManager = StudyTaskManager.getInstance(project);
    for (AnswerPlaceholder answerPlaceholder : selectedTaskFile.getAnswerPlaceholders()) {
      answerPlaceholder.reset();
      studyTaskManager.setStatus(answerPlaceholder, CheckStatus.Unchecked);
    }
  }


  static void resetDocument(@NotNull final Document document,
                            @NotNull final TaskFile taskFile) {
    EduUtils.deleteGuardedBlocks(document);
    taskFile.setTrackChanges(false);
    clearDocument(document);

    document.setText(taskFile.text);
    taskFile.setTrackChanges(true);
  }

  private static void clearDocument(@NotNull final Document document) {
    final int lineCount = document.getLineCount();
    if (lineCount != 0) {
      CommandProcessor.getInstance().runUndoTransparentAction(() -> document.deleteString(0, document.getLineEndOffset(lineCount - 1)));
    }
  }

  public void actionPerformed(@NotNull AnActionEvent event) {
    final Project project = event.getProject();
    if (project != null) {
      refresh(project);
    }
  }

  @Override
  public void update(AnActionEvent event) {
    EduUtils.updateAction(event);
    final Project project = event.getProject();
    if (project != null) {
      EduEditor eduEditor = EduUtils.getSelectedEduEditor(project);
      EduState eduState = new EduState(eduEditor);
      Presentation presentation = event.getPresentation();
      if (!eduState.isValid()) {
        presentation.setEnabled(false);
        return;
      }

      Course course = StudyTaskManager.getInstance(project).getCourse();
      if (course == null) {
        return;
      }
      Task task = EduUtils.getCurrentTask(project);
      if (task == null) {
        return;
      }
      presentation.setText(task instanceof EduTask ? TEXT : RESET_TASK);
      if (!course.isStudy()) {
        presentation.setVisible(true);
        presentation.setEnabled(false);
      }
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
