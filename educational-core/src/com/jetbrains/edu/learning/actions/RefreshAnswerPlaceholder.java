package com.jetbrains.edu.learning.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.jetbrains.edu.learning.EduState;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.OpenApiExtKt;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.TaskFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RefreshAnswerPlaceholder extends DumbAwareAction {

  public static final String NAME = "Refresh Answer Placeholder";

  public RefreshAnswerPlaceholder() {
    super(NAME, NAME, AllIcons.Actions.Refresh);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    if (project == null) {
      return;
    }
    final AnswerPlaceholder placeholder = getAnswerPlaceholder(e);
    if (placeholder == null) {
      return;
    }
    Editor editor = OpenApiExtKt.getSelectedEditor(project);
    if (editor != null) {
      EduUtils.replaceAnswerPlaceholder(editor.getDocument(), placeholder);
      placeholder.reset(false);
    }
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
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
    final EduState eduState = OpenApiExtKt.getEduState(project);
    if (eduState == null) {
      return null;
    }
    final Editor editor = eduState.getEditor();
    final TaskFile taskFile = eduState.getTaskFile();
    return taskFile.getAnswerPlaceholder(editor.getCaretModel().getOffset());
  }
}
