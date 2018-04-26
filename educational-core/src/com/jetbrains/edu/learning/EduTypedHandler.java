package com.jetbrains.edu.learning;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ReadOnlyFragmentModificationException;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.editor.actionSystem.EditorWriteActionHandler;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder;
import com.jetbrains.edu.learning.courseFormat.TaskFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Used to forbid placeholder deletion
 */
public class EduTypedHandler extends EditorWriteActionHandler {
  protected final EditorActionHandler myOriginalHandler;

  public EduTypedHandler(EditorActionHandler originalHandler) {
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

    final int start = editor.getSelectionModel().getSelectionStart();
    final int end = editor.getSelectionModel().getSelectionEnd();
    AnswerPlaceholder placeholder = getAnswerPlaceholder(start, end, taskFile.getAnswerPlaceholders());
    if (placeholder != null && editor.getSelectionModel().hasSelection()) {
      throw new ReadOnlyFragmentModificationException(null, null);
    }
    placeholder = taskFile.getAnswerPlaceholder(currentCaret.getOffset());
    if (placeholder != null && placeholder.getRealLength() == 1) {
      throw new ReadOnlyFragmentModificationException(null, null);
    }
    else {
      myOriginalHandler.execute(editor, caret, dataContext);
    }
  }

  @Nullable
  public static AnswerPlaceholder getAnswerPlaceholder(int start, int end, List<AnswerPlaceholder> placeholders) {
    for (AnswerPlaceholder placeholder : placeholders) {
      int placeholderStart = placeholder.getOffset();
      int placeholderEnd = placeholderStart + placeholder.getRealLength();
      if (placeholderStart >= start && placeholderStart < end && placeholderEnd <= end &&
          placeholderEnd > start) {
        return placeholder;
      }
    }
    return null;
  }

  @Nullable
  public static TaskFile getTaskFile(@Nullable Editor editor) {
    if (editor == null) return null;
    final Project project = editor.getProject();
    if (project == null) return null;
    final VirtualFile openedFile = FileDocumentManager.getInstance().getFile(editor.getDocument());
    if (openedFile == null) return null;
    final TaskFile taskFile = EduUtils.getTaskFile(project, openedFile);
    if (taskFile == null) return null;

    return taskFile;
  }
}

