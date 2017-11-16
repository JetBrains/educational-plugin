package com.jetbrains.edu.learning.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.jetbrains.edu.learning.StudyState;
import com.jetbrains.edu.learning.StudySubtaskUtils;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.StudyUtils;
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.CheckStatus;
import com.jetbrains.edu.learning.courseFormat.TaskFile;
import com.jetbrains.edu.learning.editor.EduEditor;
import org.jetbrains.annotations.Nullable;

public class RefreshAnswerPlaceholder extends DumbAwareAction {

  public static final String NAME = "Refresh Answer Placeholder";

  public RefreshAnswerPlaceholder() {
    super(NAME, NAME, AllIcons.Actions.Refresh);
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    Project project = e.getProject();
    if (project == null) {
      return;
    }
    final AnswerPlaceholder answerPlaceholder = getAnswerPlaceholder(e);
    if (answerPlaceholder == null) {
      return;
    }
    EduEditor eduEditor = StudyUtils.getSelectedStudyEditor(project);
    if (eduEditor != null) {
      StudySubtaskUtils.refreshPlaceholder(eduEditor.getEditor(), answerPlaceholder);
      final StudyTaskManager taskManager = StudyTaskManager.getInstance(project);
      answerPlaceholder.reset();
      taskManager.setStatus(answerPlaceholder, CheckStatus.Unchecked);
    }
  }

  @Override
  public void update(AnActionEvent e) {
    Presentation presentation = e.getPresentation();
    presentation.setEnabledAndVisible(false);
    Project project = e.getProject();
    if (project == null) {
      return;
    }
    Course course = StudyTaskManager.getInstance(project).getCourse();
    if (course == null) {
      return;
    }

    if (!course.isStudy()) {
      presentation.setVisible(true);
      return;
    }

    if (getAnswerPlaceholder(e) == null) {
      presentation.setEnabledAndVisible(false);
      return;
    }
    presentation.setEnabledAndVisible(true);
  }

  @Nullable
  private static AnswerPlaceholder getAnswerPlaceholder(AnActionEvent e) {
    final Project project = e.getProject();
    if (project == null) {
      return null;
    }
    EduEditor eduEditor = StudyUtils.getSelectedStudyEditor(project);
    final StudyState studyState = new StudyState(eduEditor);
    if (eduEditor == null || !studyState.isValid()) {
      return null;
    }
    final Editor editor = studyState.getEditor();
    final TaskFile taskFile = studyState.getTaskFile();
    return taskFile.getAnswerPlaceholder(editor.getCaretModel().getOffset());
  }
}
