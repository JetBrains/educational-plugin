package com.jetbrains.edu.learning.editor;


import com.intellij.ide.projectView.ProjectView;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.event.EditorFactoryEvent;
import com.intellij.openapi.editor.event.EditorFactoryListener;
import com.intellij.openapi.editor.event.EditorMouseEvent;
import com.intellij.openapi.editor.event.EditorMouseListener;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.problems.WolfTheProblemSolver;
import com.jetbrains.edu.learning.*;
import com.jetbrains.edu.learning.courseFormat.*;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask;
import com.jetbrains.edu.learning.navigation.NavigationUtils;
import com.jetbrains.edu.learning.placeholderDependencies.PlaceholderDependencyManager;
import com.jetbrains.edu.learning.statistics.EduLaunchesReporter;
import com.jetbrains.edu.learning.stepik.api.StepikConnectorUtils;
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse;
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionToolWindowFactory;
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionView;
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

import static com.jetbrains.edu.learning.stepik.hyperskill.HyperskillUtilsKt.markTheoryTaskAsCompleted;

public class EduEditorFactoryListener implements EditorFactoryListener {

  private static final Logger LOG = Logger.getInstance(EduEditorFactoryListener.class);

  private static class WindowSelectionListener implements EditorMouseListener {
    private final TaskFile myTaskFile;

    public WindowSelectionListener(TaskFile file) {
      myTaskFile = file;
    }

    @Override
    public void mouseClicked(@NotNull EditorMouseEvent e) {
      final Editor editor = e.getEditor();
      final Point point = e.getMouseEvent().getPoint();
      final LogicalPosition pos = editor.xyToLogicalPosition(point);
      final AnswerPlaceholder answerPlaceholder = myTaskFile.getAnswerPlaceholder(editor.logicalPositionToOffset(pos));
      if (answerPlaceholder == null || !answerPlaceholder.isVisible() || answerPlaceholder.getSelected()) {
        return;
      }
      final Pair<Integer, Integer> offsets = EduUtils.getPlaceholderOffsets(answerPlaceholder);
      editor.getSelectionModel().setSelection(offsets.getFirst(), offsets.getSecond());
      answerPlaceholder.setSelected(true);
      YamlFormatSynchronizer.saveItem(myTaskFile.getTask());
    }
  }

  @Override
  public void editorCreated(@NotNull final EditorFactoryEvent event) {
    final Editor editor = event.getEditor();
    final Project project = editor.getProject();
    if (project == null) {
      return;
    }
    Course course = StudyTaskManager.getInstance(project).getCourse();
    if (course == null) {
      return;
    }

    final Document document = editor.getDocument();
    final VirtualFile openedFile = FileDocumentManager.getInstance().getFile(document);
    if (openedFile != null) {
      TaskFile taskFile = VirtualFileExt.getTaskFile(openedFile, project);
      if (taskFile != null) {
        WolfTheProblemSolver.getInstance(project).clearProblems(openedFile);
        showTaskDescriptionToolWindow(project, taskFile, true);

        Task task = taskFile.getTask();
        markViewed(project, task);

        boolean isStudyProject = course.isStudy();
        if (!taskFile.getAnswerPlaceholders().isEmpty() && taskFile.isValid(editor.getDocument().getText())) {
          PlaceholderDependencyManager.updateDependentPlaceholders(project, task);
          NavigationUtils.navigateToFirstAnswerPlaceholder(editor, taskFile);
          PlaceholderPainter.showPlaceholders(project, taskFile, editor);
          if (isStudyProject) {
            editor.addEditorMouseListener(new WindowSelectionListener(taskFile));
          }
        }
        EduLaunchesReporter.INSTANCE.sendStats(course);
      }
    }
  }

  @Override
  public void editorReleased(@NotNull EditorFactoryEvent event) {
    event.getEditor().getSelectionModel().removeSelection();
  }

  private static void showTaskDescriptionToolWindow(@NotNull Project project, @NotNull TaskFile taskFile, boolean retry) {
    ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
    final ToolWindow studyToolWindow = toolWindowManager.getToolWindow(TaskDescriptionToolWindowFactory.STUDY_TOOL_WINDOW);
    if (studyToolWindow == null) {
      if (retry) {
        toolWindowManager.invokeLater(() -> showTaskDescriptionToolWindow(project, taskFile, false));
      }
      else {
        LOG.warn(String.format("Failed to get toolwindow with `%s` id", TaskDescriptionToolWindowFactory.STUDY_TOOL_WINDOW));
      }
      return;
    }

    if (!taskFile.getTask().equals(TaskDescriptionView.getInstance(project).getCurrentTask())) {
      EduUtils.updateToolWindows(project);
      studyToolWindow.show(null);
    }
  }

  private static void markViewed(@NotNull Project project, @NotNull Task task) {
    if (!(task instanceof TheoryTask)) return;
    TheoryTask theoryTask = (TheoryTask) task;
    Course course = theoryTask.getCourse();
    if (course.isStudy() && theoryTask.postSubmissionOnOpen && theoryTask.getStatus() != CheckStatus.Solved) {
      if (course instanceof HyperskillCourse) {
        markTheoryTaskAsCompleted(project, theoryTask);
      }
      else if (course instanceof EduCourse && ((EduCourse)course).isStepikRemote() && EduSettings.isLoggedIn()) {
        StepikConnectorUtils.postTheory(theoryTask, project);
      }

      theoryTask.setStatus(CheckStatus.Solved);
      YamlFormatSynchronizer.saveItem(theoryTask);
      ProjectView.getInstance(project).refresh();
    }
  }
}
