package com.jetbrains.edu.coursecreator.actions.placeholder;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.JBColor;
import com.intellij.util.DocumentUtil;
import com.jetbrains.edu.learning.AnswerPlaceholderPainter;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder;
import com.jetbrains.edu.learning.courseFormat.TaskFile;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CCAddAnswerPlaceholder extends CCAnswerPlaceholderAction {

  public CCAddAnswerPlaceholder() {
    super("Add Answer Placeholder", "Add/Delete answer placeholder");
  }


  private static boolean arePlaceholdersIntersect(@NotNull final TaskFile taskFile, int start, int end) {
    List<AnswerPlaceholder> answerPlaceholders = taskFile.getAnswerPlaceholders();
    for (AnswerPlaceholder existingAnswerPlaceholder : answerPlaceholders) {
      int twStart = existingAnswerPlaceholder.getOffset();
      int twEnd = existingAnswerPlaceholder.getPossibleAnswerLength() + twStart;
      if ((start >= twStart && start < twEnd) || (end > twStart && end <= twEnd) ||
          (twStart >= start && twStart < end) || (twEnd > start && twEnd <= end)) {
        return true;
      }
    }
    return false;
  }

  private void addPlaceholder(@NotNull CCState state) {
    Editor editor = state.getEditor();
    Project project = state.getProject();
    Document document = editor.getDocument();
    FileDocumentManager.getInstance().saveDocument(document);
    final SelectionModel model = editor.getSelectionModel();
    final int offset = model.hasSelection() ? model.getSelectionStart() : editor.getCaretModel().getOffset();
    TaskFile taskFile = state.getTaskFile();
    final AnswerPlaceholder answerPlaceholder = new AnswerPlaceholder();
    int index = taskFile.getAnswerPlaceholders().size();
    answerPlaceholder.setIndex(index);
    answerPlaceholder.setTaskFile(taskFile);
    taskFile.sortAnswerPlaceholders();
    answerPlaceholder.setOffset(offset);
    answerPlaceholder.setUseLength(false);

    String defaultPlaceholderText = "type here";
    CCCreateAnswerPlaceholderDialog dlg = createDialog(project, answerPlaceholder);
    if (!dlg.showAndGet()) {
      return;
    }
    String answerPlaceholderText = dlg.getTaskText();
    answerPlaceholder.setPossibleAnswer(model.hasSelection() ? model.getSelectedText() : defaultPlaceholderText);
    answerPlaceholder.setPlaceholderText(StringUtil.notNullize(answerPlaceholderText));
    answerPlaceholder.setLength(StringUtil.notNullize(answerPlaceholderText).length());
    answerPlaceholder.setHints(dlg.getHints());

    if (!model.hasSelection()) {
      DocumentUtil.writeInRunUndoTransparentAction(() -> document.insertString(offset, defaultPlaceholderText));
    }

    answerPlaceholder.setPossibleAnswer(model.hasSelection() ? model.getSelectedText() : defaultPlaceholderText);
    AddAction action = new AddAction(answerPlaceholder, taskFile, editor);
    EduUtils.runUndoableAction(project, "Add Answer Placeholder", action);
  }

  static class AddAction extends TaskFileUndoableAction {
    private final AnswerPlaceholder myPlaceholder;

    public AddAction(@NotNull AnswerPlaceholder placeholder, @NotNull TaskFile taskFile, @NotNull Editor editor) {
      super(taskFile, editor);
      myPlaceholder = placeholder;
    }

    @Override
    public void performUndo() {
      final List<AnswerPlaceholder> answerPlaceholders = getTaskFile().getAnswerPlaceholders();
      if (answerPlaceholders.contains(myPlaceholder)) {
        answerPlaceholders.remove(myPlaceholder);
        getEditor().getMarkupModel().removeAllHighlighters();
        EduUtils.drawAllAnswerPlaceholders(getEditor(), getTaskFile());
        AnswerPlaceholderPainter.createGuardedBlocks(getEditor(), getTaskFile());
      }
    }

    @Override
    public void performRedo() {
      getTaskFile().addAnswerPlaceholder(myPlaceholder);
      AnswerPlaceholderPainter.drawAnswerPlaceholder(getEditor(), myPlaceholder, JBColor.BLUE);
      AnswerPlaceholderPainter.createGuardedBlocks(getEditor(), myPlaceholder);
    }
  }

  @Override
  protected void performAnswerPlaceholderAction(@NotNull CCState state) {
    addPlaceholder(state);
  }

  @Override
  public void update(@NotNull AnActionEvent event) {
    final Presentation presentation = event.getPresentation();
    presentation.setEnabledAndVisible(false);

    CCState state = getState(event);
    if (state == null) {
      return;
    }

    presentation.setVisible(true);
    if (canAddPlaceholder(state)) {
      presentation.setEnabled(true);
    }
  }


  private static boolean canAddPlaceholder(@NotNull CCState state) {
    Editor editor = state.getEditor();
    SelectionModel selectionModel = editor.getSelectionModel();
    TaskFile taskFile = state.getTaskFile();
    if (selectionModel.hasSelection()) {
      int start = selectionModel.getSelectionStart();
      int end = selectionModel.getSelectionEnd();
      return !arePlaceholdersIntersect(taskFile, start, end);
    }
    int offset = editor.getCaretModel().getOffset();
    return EduUtils.getAnswerPlaceholder(offset, taskFile.getAnswerPlaceholders()) == null;
  }

  protected CCCreateAnswerPlaceholderDialog createDialog(Project project, AnswerPlaceholder answerPlaceholder) {
    String answerPlaceholderText = StringUtil.notNullize(answerPlaceholder.getPlaceholderText());
    return new CCCreateAnswerPlaceholderDialog(project, answerPlaceholderText.isEmpty() ? "type here" : answerPlaceholderText,
                                               answerPlaceholder.getHints());
  }
}