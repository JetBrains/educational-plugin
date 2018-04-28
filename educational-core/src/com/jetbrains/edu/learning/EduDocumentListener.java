package com.jetbrains.edu.learning;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.impl.event.DocumentEventImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder;
import com.jetbrains.edu.learning.courseFormat.TaskFile;
import org.jetbrains.annotations.NotNull;

/**
 * Listens changes in study files and updates
 * coordinates of all the placeholders in current task file
 */
public class EduDocumentListener implements DocumentListener {
  protected final Project myProject;
  protected final TaskFile myTaskFile;

  public EduDocumentListener(Project project, TaskFile taskFile) {
    myProject = project;
    myTaskFile = taskFile;
  }

  @Override
  public void beforeDocumentChange(DocumentEvent e) {
    if (!myTaskFile.isTrackChanges()) {
      return;
    }
    myTaskFile.setHighlightErrors(true);
  }

  @Override
  public void documentChanged(DocumentEvent e) {
    if (!myTaskFile.isTrackChanges()) {
      return;
    }
    if (myTaskFile.getAnswerPlaceholders().isEmpty()) return;

    if (!(e instanceof DocumentEventImpl)) {
      return;
    }

    DocumentEventImpl event = (DocumentEventImpl)e;
    Document document = e.getDocument();

    int offset = e.getOffset();
    int change = event.getNewLength() - event.getOldLength();

    final CharSequence fragment = e.getNewFragment();
    CharSequence oldFragment = e.getOldFragment();

    for (AnswerPlaceholder placeholder : myTaskFile.getAnswerPlaceholders()) {
      int placeholderStart = placeholder.getOffset();
      if (placeholderStart - 1 == offset && fragment.toString().isEmpty() && oldFragment.toString().startsWith("\n")) {
        placeholderStart -= 1;
      }

      if (placeholderStart > offset) {
        placeholderStart += change;
      }
      int placeholderEnd = placeholder.getEndOffset();
      if (placeholderEnd >= offset) {
        placeholderEnd += change;
      }
      if (placeholderStart == offset && oldFragment.toString().isEmpty() && fragment.toString().startsWith("\n")) {
        placeholderStart += 1;
      }

      int length = placeholderEnd - placeholderStart;
      assert length >= 0;
      assert placeholderStart >= 0;
      updatePlaceholder(placeholder, document, placeholderStart, length);
    }
  }

  protected void updatePlaceholder(@NotNull AnswerPlaceholder answerPlaceholder,
                                   @NotNull Document document, int start, int length) {
    answerPlaceholder.setOffset(start);
    if (answerPlaceholder.getUseLength()) {
      answerPlaceholder.setLength(length);
    } else {
      if (myTaskFile.isTrackLengths()) {
        answerPlaceholder.setPossibleAnswer(document.getText(TextRange.create(start, start + length)));
      }
    }
  }
}

