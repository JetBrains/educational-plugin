package com.jetbrains.edu.learning;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ReadOnlyFragmentModificationException;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.editor.actionSystem.EditorWriteActionHandler;
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder;
import com.jetbrains.edu.learning.courseFormat.TaskFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.jetbrains.edu.learning.EduTypedHandler.getAnswerPlaceholder;
import static com.jetbrains.edu.learning.EduTypedHandler.getTaskFile;

/**
 * Used to forbid placeholder deletion while executing line actions
 */
public class EduTypedLineHandler extends EditorWriteActionHandler {
  protected final EditorActionHandler myOriginalHandler;

  public EduTypedLineHandler(EditorActionHandler originalHandler) {
    super(false);
    myOriginalHandler = originalHandler;
  }

  @Override
  protected boolean isEnabledForCaret(@NotNull Editor editor, @NotNull Caret caret, DataContext dataContext) {
    return true;
  }

  @Override
  public void executeWriteAction(Editor editor, @Nullable Caret caret, DataContext dataContext) {
    final Caret currentCaret = editor.getCaretModel().getPrimaryCaret();
    final TaskFile taskFile = getTaskFile(editor);
    if (taskFile == null) {
      myOriginalHandler.execute(editor, caret, dataContext);
      return;
    }

    final Document document = editor.getDocument();
    final int lineNumber = document.getLineNumber(currentCaret.getOffset());
    int lineEndOffset = document.getLineEndOffset(lineNumber);
    int lineStartOffset = document.getLineStartOffset(lineNumber);
    final AnswerPlaceholder placeholder = getAnswerPlaceholder(lineStartOffset, lineEndOffset, taskFile.getAnswerPlaceholders());
    if (placeholder != null) {
      throw new ReadOnlyFragmentModificationException(null, null);
    }
    else {
      myOriginalHandler.execute(editor, caret, dataContext);
    }
  }
}

