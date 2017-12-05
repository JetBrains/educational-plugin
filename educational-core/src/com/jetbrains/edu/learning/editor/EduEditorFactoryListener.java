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
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.EduDocumentListener;
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.TaskFile;
import com.jetbrains.edu.learning.navigation.NavigationUtils;
import com.jetbrains.edu.learning.statistics.EduLaunchesReporter;
import com.jetbrains.edu.learning.ui.taskDescription.TaskDescriptionToolWindowFactory;
import org.jetbrains.annotations.NotNull;

import java.awt.*;


public class EduEditorFactoryListener implements EditorFactoryListener {

  private static class WindowSelectionListener extends EditorMouseAdapter {
    private final TaskFile myTaskFile;

    WindowSelectionListener(@NotNull final TaskFile taskFile) {
      myTaskFile = taskFile;
    }

    @Override
    public void mouseClicked(EditorMouseEvent e) {
      final Editor editor = e.getEditor();
      final Point point = e.getMouseEvent().getPoint();
      final LogicalPosition pos = editor.xyToLogicalPosition(point);
      final AnswerPlaceholder answerPlaceholder = myTaskFile.getAnswerPlaceholder(editor.logicalPositionToOffset(pos));
      if (answerPlaceholder == null || answerPlaceholder.getSelected()) {
        return;
      }
      final Pair<Integer, Integer> offsets = EduUtils.getPlaceholderOffsets(answerPlaceholder, editor.getDocument());
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
      final TaskFile taskFile = EduUtils.getTaskFile(project, openedFile);
      if (taskFile != null) {
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

        EduEditor.addDocumentListener(document, new EduDocumentListener(taskFile, true));

        if (!taskFile.getAnswerPlaceholders().isEmpty() && taskFile.isValid(editor.getDocument().getText())) {
          NavigationUtils.navigateToFirstAnswerPlaceholder(editor, taskFile);
          boolean isStudyProject = course.isStudy();
          EduUtils.drawAllAnswerPlaceholders(editor, taskFile);
          if (isStudyProject) {
            editor.addEditorMouseListener(new WindowSelectionListener(taskFile));
          }
        }
        EduLaunchesReporter.INSTANCE.sendStats();
      }
    }
  }

  @Override
  public void editorReleased(@NotNull EditorFactoryEvent event) {
    final Editor editor = event.getEditor();
    final Document document = editor.getDocument();
    EduEditor.removeListener(document);
    editor.getMarkupModel().removeAllHighlighters();
    editor.getSelectionModel().removeSelection();
  }
}
