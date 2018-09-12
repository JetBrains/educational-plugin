package com.jetbrains.edu.learning.editor;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.event.EditorFactoryEvent;
import com.intellij.openapi.editor.event.EditorFactoryListener;
import com.intellij.openapi.editor.event.EditorMouseAdapter;
import com.intellij.openapi.editor.event.EditorMouseEvent;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.problems.WolfTheProblemSolver;
import com.jetbrains.edu.coursecreator.actions.CCPluginToggleAction;
import com.jetbrains.edu.learning.EduDocumentListener;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.NewPlaceholderPainter;
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
import com.jetbrains.edu.learning.stepik.StepikConnector;
import com.jetbrains.edu.learning.ui.taskDescription.TaskDescriptionToolWindowFactory;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EduEditorFactoryListener implements EditorFactoryListener {

  private static final Map<Document, EduDocumentListener> ourDocumentListeners = new HashMap<>();

  private TaskFile myTaskFile;

  private static class WindowSelectionListener extends EditorMouseAdapter {
    private final TaskFile myTaskFile;

    public WindowSelectionListener(TaskFile file) {
      myTaskFile = file;
    }

    @Override
    public void mouseClicked(EditorMouseEvent e) {
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
      myTaskFile = EduUtils.getTaskFile(project, openedFile);
      if (myTaskFile != null) {
        WolfTheProblemSolver.getInstance(project).clearProblems(openedFile);
        final ToolWindow studyToolWindow = ToolWindowManager.getInstance(project).getToolWindow(TaskDescriptionToolWindowFactory.STUDY_TOOL_WINDOW);
        if (studyToolWindow != null) {
          EduUtils.updateToolWindows(project);
          studyToolWindow.show(null);
        }
        Course course = StudyTaskManager.getInstance(project).getCourse();
        if (course == null) {
          return;
        }

        Task task = myTaskFile.getTask();
        if (task instanceof TheoryTask && task.getStatus() != CheckStatus.Solved) {
          task.setStatus(CheckStatus.Solved);
          StepikConnector.postTheory(task, project);
        }

        addDocumentListener(document, new EduDocumentListener(project, myTaskFile));

        boolean isStudyProject = course.isStudy();
        if (!myTaskFile.getAnswerPlaceholders().isEmpty() && myTaskFile.isValid(editor.getDocument().getText())) {
          PlaceholderDependencyManager.updateDependentPlaceholders(project, task, true);
          NavigationUtils.navigateToFirstAnswerPlaceholder(editor, myTaskFile);
          EduUtils.drawAllAnswerPlaceholders(editor, myTaskFile);
          if (isStudyProject) {
            editor.addEditorMouseListener(new WindowSelectionListener(myTaskFile));
          }
        }
        EduLaunchesReporter.INSTANCE.sendStats(isStudyProject, CCPluginToggleAction.isCourseCreatorFeaturesEnabled());
      }
    }
  }

  @Override
  public void editorReleased(@NotNull EditorFactoryEvent event) {
    final Editor editor = event.getEditor();
    final Document document = editor.getDocument();
    removeDocumentListener(document);
    if (myTaskFile != null) {
      final List<AnswerPlaceholder> placeholders = myTaskFile.getAnswerPlaceholders();
      for (AnswerPlaceholder placeholder : placeholders) {
        NewPlaceholderPainter.getPlaceholderPainters().remove(placeholder);
      }
    }
    editor.getSelectionModel().removeSelection();
  }

  public static void addDocumentListener(@NotNull final Document document, @NotNull final EduDocumentListener listener) {
    document.addDocumentListener(listener);
    ourDocumentListeners.put(document, listener);
  }

  public static void removeDocumentListener(Document document) {
    final EduDocumentListener listener = ourDocumentListeners.get(document);
    if (listener != null) {
      document.removeDocumentListener(listener);
    }
    ourDocumentListeners.remove(document);
  }

  public static boolean hasDocumentListener(@NotNull final Document document) {
    return ourDocumentListeners.containsKey(document);
  }
}
