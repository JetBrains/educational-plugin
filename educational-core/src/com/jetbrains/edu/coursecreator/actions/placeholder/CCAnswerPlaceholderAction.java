package com.jetbrains.edu.coursecreator.actions.placeholder;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsActions;
import com.jetbrains.edu.coursecreator.CCUtils;
import com.jetbrains.edu.learning.EduState;
import com.jetbrains.edu.learning.OpenApiExtKt;
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson;
import com.jetbrains.edu.learning.courseFormat.Lesson;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

abstract public class CCAnswerPlaceholderAction extends DumbAwareAction {

  protected CCAnswerPlaceholderAction(@NotNull Supplier<@NlsActions.ActionText String> text,
                                      @NotNull Supplier<@NlsActions.ActionDescription String> description) {
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

    EduState state = getEduState(project);
    if (state != null) {
      updatePresentation(state, presentation);
    }
  }

  @Nullable
  private static EduState getEduState(@NotNull Project project) {
    if (!CCUtils.isCourseCreator(project)) {
      return null;
    }
    EduState state = OpenApiExtKt.getEduState(project);
    if (state == null) {
      return null;
    }
    Lesson lesson = state.getTaskFile().getTask().getLesson();
    // Disable all placeholder actions in non template based framework lessons for now
    if (lesson instanceof FrameworkLesson && !((FrameworkLesson)lesson).isTemplateBased()) {
      return null;
    }
    return state;
  }

  protected abstract void updatePresentation(@NotNull EduState eduState, @NotNull Presentation presentation);

  protected abstract void performAnswerPlaceholderAction(@NotNull Project project, final @NotNull EduState state);

  @Override
  public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.EDT;
  }
}
