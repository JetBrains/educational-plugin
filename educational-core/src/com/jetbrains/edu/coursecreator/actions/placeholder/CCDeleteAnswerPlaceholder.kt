package com.jetbrains.edu.coursecreator.actions.placeholder;

import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import com.jetbrains.edu.learning.EduState;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder;
import com.jetbrains.edu.learning.courseFormat.TaskFile;
import com.jetbrains.edu.learning.messages.EduCoreBundle;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class CCDeleteAnswerPlaceholder extends CCAnswerPlaceholderAction {

  @NonNls
  public static final String ACTION_ID = "Educational.Educator.DeleteAnswerPlaceholder";

  public CCDeleteAnswerPlaceholder() {
    super();
  }

  @Override
  protected void performAnswerPlaceholderAction(@NotNull Project project, @NotNull EduState state) {
    deletePlaceholder(project, state);
  }

  private static void deletePlaceholder(@NotNull Project project, @NotNull EduState state) {
    TaskFile taskFile = state.getTaskFile();
    AnswerPlaceholder answerPlaceholder = state.getAnswerPlaceholder();
    if (answerPlaceholder == null) {
      throw new IllegalStateException("Delete Placeholder action called, but no placeholder found");
    }

    EduUtils.runUndoableAction(project, EduCoreBundle.message("action.Educational.Educator.DeleteAnswerPlaceholder.text.full"),
                               new CCAddAnswerPlaceholder.AddAction(project, answerPlaceholder, taskFile, state.getEditor()) {
                                 @Override
                                 public void undo() {
                                   super.redo();
                                 }

                                 @Override
                                 public void redo() {
                                   super.undo();
                                 }
                               });
  }

  private static boolean canDeletePlaceholder(@NotNull EduState state) {
    if (state.getEditor().getSelectionModel().hasSelection()) {
      return false;
    }
    return state.getAnswerPlaceholder() != null;
  }

  @Override
  protected void updatePresentation(@NotNull EduState eduState, @NotNull Presentation presentation) {
    presentation.setEnabledAndVisible(canDeletePlaceholder(eduState));
  }
}
