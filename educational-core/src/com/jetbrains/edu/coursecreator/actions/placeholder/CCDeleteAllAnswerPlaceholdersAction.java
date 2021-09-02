package com.jetbrains.edu.coursecreator.actions.placeholder;

import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.command.UndoConfirmationPolicy;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.jetbrains.edu.learning.EduState;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.PlaceholderPainter;
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder;
import com.jetbrains.edu.learning.courseFormat.TaskFile;
import com.jetbrains.edu.learning.messages.EduCoreBundle;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class CCDeleteAllAnswerPlaceholdersAction extends CCAnswerPlaceholderAction {

  public static final String ACTION_ID = "Educational.Educator.DeleteAllPlaceholders";

  public CCDeleteAllAnswerPlaceholdersAction() {
    super(() -> EduCoreBundle.message("action.delete.all.answer.placeholders.text"),
          () -> EduCoreBundle.message("action.delete.all.answer.placeholders.description"));
  }

  @Override
  protected void updatePresentation(@NotNull EduState eduState, @NotNull Presentation presentation) {
    presentation.setEnabledAndVisible(eduState.getTaskFile().getAnswerPlaceholders().size() > 1);
  }

  @Override
  protected void performAnswerPlaceholderAction(@NotNull Project project, @NotNull EduState state) {
    final ClearPlaceholders action = new ClearPlaceholders(project, state.getTaskFile(), state.getEditor());
    EduUtils.runUndoableAction(project, EduCoreBundle.message("action.delete.all.answer.placeholders.text"), action,
                               UndoConfirmationPolicy.REQUEST_CONFIRMATION);
  }

  private static class ClearPlaceholders extends TaskFileUndoableAction {
    private final List<AnswerPlaceholder> myPlaceholders;
    private final TaskFile myTaskFile;

    public ClearPlaceholders(@NotNull Project project, @NotNull TaskFile taskFile, @NotNull Editor editor) {
      super(project, taskFile, editor);
      myTaskFile = taskFile;
      myPlaceholders = new ArrayList<>(taskFile.getAnswerPlaceholders());
    }

    @Override
    public boolean performUndo() {
      myTaskFile.getAnswerPlaceholders().addAll(myPlaceholders);
      PlaceholderPainter.showPlaceholders(getProject(), myTaskFile);
      return true;
    }

    @Override
    public void performRedo() {
      PlaceholderPainter.hidePlaceholders(myTaskFile);
      myTaskFile.getAnswerPlaceholders().clear();
    }

    @Override
    public boolean isGlobal() {
      return true;
    }
  }
}
