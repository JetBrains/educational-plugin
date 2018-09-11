package com.jetbrains.edu.learning.editor;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ReadOnlyFragmentModificationException;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder;
import com.jetbrains.edu.learning.courseFormat.TaskFile;
import org.jetbrains.annotations.Nullable;

public class EduCutTypedHandler extends EduTypedHandler {

  public EduCutTypedHandler(EditorActionHandler originalHandler) {
    super(originalHandler);
  }

  @Override
  public void executeWriteAction(Editor editor, @Nullable Caret caret, DataContext dataContext) {
    final Caret currentCaret = editor.getCaretModel().getPrimaryCaret();
    final TaskFile taskFile = getTaskFile(editor);
    if (taskFile == null) {
      myOriginalHandler.execute(editor, caret, dataContext);
      return;
    }

    final int start = editor.getSelectionModel().getSelectionStart();
    final int end = editor.getSelectionModel().getSelectionEnd();
    AnswerPlaceholder placeholder = getAnswerPlaceholder(start, end, taskFile.getAnswerPlaceholders());
    if (placeholder != null && editor.getSelectionModel().hasSelection()) {
      throw new ReadOnlyFragmentModificationException(null, null);
    }
    placeholder = taskFile.getAnswerPlaceholder(currentCaret.getOffset());
    if (placeholder != null && start == end) {
      throw new ReadOnlyFragmentModificationException(null, null);
    }
    else {
      myOriginalHandler.execute(editor, caret, dataContext);
    }
  }
}

