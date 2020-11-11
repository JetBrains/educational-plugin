package com.jetbrains.edu.coursecreator.actions.placeholder;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.jetbrains.edu.coursecreator.CCUtils;
import com.jetbrains.edu.learning.EduState;
import com.jetbrains.edu.learning.OpenApiExtKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

abstract public class CCAnswerPlaceholderAction extends DumbAwareAction {

  protected CCAnswerPlaceholderAction(@NotNull Supplier<String> text, @NotNull Supplier<String> description) {
    super(text, description, null);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    if (project == null) {
      return;
    }

    EduState eduState = getEduState(project);
    if (eduState == null) {
      return;
    }
    performAnswerPlaceholderAction(project, eduState);
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    Presentation presentation = e.getPresentation();
    presentation.setEnabledAndVisible(false);
    Project project = e.getProject();
    if (project == null) {
      return;
    }

    if (!CCUtils.isCourseCreator(project)) {
      return;
    }

    EduState state = OpenApiExtKt.getEduState(project);
    if (state != null) {
      updatePresentation(state, presentation);
    }
  }

  @Nullable
  private static EduState getEduState(@NotNull Project project) {
    if (!CCUtils.isCourseCreator(project)) {
      return null;
    }
    return OpenApiExtKt.getEduState(project);
  }

  protected abstract void updatePresentation(@NotNull EduState eduState, @NotNull Presentation presentation);

  protected abstract void performAnswerPlaceholderAction(@NotNull Project project, final @NotNull EduState state);
}
