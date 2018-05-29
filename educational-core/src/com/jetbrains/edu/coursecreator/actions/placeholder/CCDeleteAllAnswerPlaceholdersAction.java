package com.jetbrains.edu.coursecreator.actions.placeholder;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.command.UndoConfirmationPolicy;
import com.intellij.openapi.editor.Editor;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.NewPlaceholderPainter;
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder;
import com.jetbrains.edu.learning.courseFormat.TaskFile;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class CCDeleteAllAnswerPlaceholdersAction extends CCAnswerPlaceholderAction {

  public static final String ACTION_NAME = "Delete All";
  public static final String ACTION_DESCRIPTION = "Delete all placeholders in current file";

  public CCDeleteAllAnswerPlaceholdersAction() {
    super(ACTION_NAME, ACTION_DESCRIPTION);
  }

  @Override
  public void update(AnActionEvent e) {
    e.getPresentation().setEnabledAndVisible(false);
    CCState state = getState(e);
    if (state == null) return;
    e.getPresentation().setEnabledAndVisible(true);
    super.update(e);
  }

  @Override
  protected void performAnswerPlaceholderAction(@NotNull CCState state) {
    List<AnswerPlaceholder> placeholders = new ArrayList<>(state.getTaskFile().getAnswerPlaceholders());
    final ClearPlaceholders action = new ClearPlaceholders(state.getTaskFile(), placeholders, state.getEditor());
    EduUtils.runUndoableAction(state.getProject(), ACTION_NAME, action, UndoConfirmationPolicy.REQUEST_CONFIRMATION);
  }

  private static void updateView(@NotNull final Editor editor,
                                 @NotNull final TaskFile taskFile) {
    EduUtils.drawAllAnswerPlaceholders(editor, taskFile);
  }

  private static class ClearPlaceholders extends TaskFileUndoableAction {
    private final List<AnswerPlaceholder> myPlaceholders;
    private final Editor myEditor;
    private final TaskFile myTaskFile;

    public ClearPlaceholders(TaskFile taskFile, List<AnswerPlaceholder> placeholders, Editor editor) {
      super(taskFile, editor);
      myTaskFile = taskFile;
      myPlaceholders = placeholders;
      myEditor = editor;
    }

    @Override
    public void performUndo() {
      myTaskFile.getAnswerPlaceholders().addAll(myPlaceholders);
      updateView(myEditor, myTaskFile);
    }

    @Override
    public void performRedo() {
      List<AnswerPlaceholder> placeholders = myTaskFile.getAnswerPlaceholders();
      for (AnswerPlaceholder placeholder : placeholders) {
        NewPlaceholderPainter.removePainter(myEditor, placeholder);
      }
      placeholders.clear();
      updateView(myEditor, myTaskFile);
    }

    @Override
    public boolean isGlobal() {
      return true;
    }
  }
}
