package com.jetbrains.edu.learning.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.jetbrains.edu.learning.EduState;
import com.jetbrains.edu.learning.SubtaskUtils;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.EduUtils;
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
    EduEditor eduEditor = EduUtils.getSelectedEduEditor(project);
    if (eduEditor != null) {
      SubtaskUtils.refreshPlaceholder(eduEditor.getEditor(), answerPlaceholder);
      final StudyTaskManager studyTaskManager = StudyTaskManager.getInstance(project);
      answerPlaceholder.reset();
      studyTaskManager.setStatus(answerPlaceholder, CheckStatus.Unchecked);
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
    EduEditor eduEditor = EduUtils.getSelectedEduEditor(project);
    final EduState eduState = new EduState(eduEditor);
    if (eduEditor == null || !eduState.isValid()) {
      return null;
    }
    final Editor editor = eduState.getEditor();
    final TaskFile taskFile = eduState.getTaskFile();
    if (taskFile == null || editor == null) return null;
    return taskFile.getAnswerPlaceholder(editor.getCaretModel().getOffset());
  }
}
