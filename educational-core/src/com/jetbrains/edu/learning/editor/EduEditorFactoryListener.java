package com.jetbrains.edu.learning.editor;


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
import com.jetbrains.edu.coursecreator.actions.CCPluginToggleAction;
import com.jetbrains.edu.learning.EduSettings;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.PlaceholderPainter;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder;
import com.jetbrains.edu.learning.courseFormat.CheckStatus;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.TaskFile;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask;
import com.jetbrains.edu.learning.navigation.NavigationUtils;
import com.jetbrains.edu.learning.placeholderDependencies.PlaceholderDependencyManager;
import com.jetbrains.edu.learning.statistics.EduLaunchesReporter;
import com.jetbrains.edu.learning.stepik.api.StepikConnectorUtils;
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionToolWindowFactory;
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class EduEditorFactoryListener implements EditorFactoryListener {

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

    final Document document = editor.getDocument();
    final VirtualFile openedFile = FileDocumentManager.getInstance().getFile(document);
    if (openedFile != null) {
      TaskFile taskFile = EduUtils.getTaskFile(project, openedFile);
      if (taskFile != null) {
        WolfTheProblemSolver.getInstance(project).clearProblems(openedFile);
        final ToolWindow studyToolWindow =
          ToolWindowManager.getInstance(project).getToolWindow(TaskDescriptionToolWindowFactory.STUDY_TOOL_WINDOW);
        if (studyToolWindow != null) {
          EduUtils.updateToolWindows(project);
          studyToolWindow.show(null);
        }
        Course course = StudyTaskManager.getInstance(project).getCourse();
        if (course == null) {
          return;
        }

        Task task = taskFile.getTask();
        if (task instanceof TheoryTask && task.getStatus() != CheckStatus.Solved) {
          task.setStatus(CheckStatus.Solved);
          if (EduSettings.isLoggedIn()) {
            StepikConnectorUtils.postTheory(task, project);
          }
        }

        boolean isStudyProject = course.isStudy();
        if (!taskFile.getAnswerPlaceholders().isEmpty() && taskFile.isValid(editor.getDocument().getText())) {
          PlaceholderDependencyManager.updateDependentPlaceholders(project, task);
          NavigationUtils.navigateToFirstAnswerPlaceholder(editor, taskFile);
          PlaceholderPainter.showPlaceholders(project, taskFile, editor);
          if (isStudyProject) {
            editor.addEditorMouseListener(new WindowSelectionListener(taskFile));
          }
        }
        EduLaunchesReporter.INSTANCE.sendStats(isStudyProject, CCPluginToggleAction.isCourseCreatorFeaturesEnabled());
      }
    }
  }

  @Override
  public void editorReleased(@NotNull EditorFactoryEvent event) {
    event.getEditor().getSelectionModel().removeSelection();
  }


}
