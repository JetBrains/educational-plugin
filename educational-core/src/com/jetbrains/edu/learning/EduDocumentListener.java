package com.jetbrains.edu.learning;

import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.impl.event.DocumentEventImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.jetbrains.edu.coursecreator.yaml.YamlFormatSynchronizer;
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
  private final boolean updateYaml;

  public EduDocumentListener(Project project, TaskFile taskFile, boolean updateYaml) {
    myProject = project;
    myTaskFile = taskFile;
    this.updateYaml = updateYaml;
  }

  @Override
  public void beforeDocumentChange(@NotNull DocumentEvent e) {
    if (!myTaskFile.isTrackChanges()) {
      return;
    }
    myTaskFile.setHighlightErrors(true);
  }

  @Override
  public void documentChanged(@NotNull DocumentEvent e) {
    if (!myTaskFile.isTrackChanges()) {
      return;
    }
    if (myTaskFile.getAnswerPlaceholders().isEmpty()) return;

    if (!(e instanceof DocumentEventImpl)) {
      return;
    }

    int offset = e.getOffset();
    int change = e.getNewLength() - e.getOldLength();

    final CharSequence fragment = e.getNewFragment();
    CharSequence oldFragment = e.getOldFragment();

    for (AnswerPlaceholder placeholder : myTaskFile.getAnswerPlaceholders()) {
      int placeholderStart = placeholder.getOffset();
      int placeholderEnd = placeholder.getEndOffset();

      final Pair<Integer, Integer> changes = getChangeForOffsets(offset, change, placeholder);
      int changeForStartOffset = changes.getFirst();
      int changeForEndOffset = changes.getSecond();

      placeholderStart += changeForStartOffset;
      placeholderEnd += changeForEndOffset;

      if (placeholderStart - 1 == offset && fragment.toString().isEmpty() && oldFragment.toString().startsWith("\n")) {
        placeholderStart -= 1;
      }

      if (placeholderStart == offset && oldFragment.toString().isEmpty() && fragment.toString().startsWith("\n")) {
        placeholderStart += 1;
      }

      int length = placeholderEnd - placeholderStart;
      assert length >= 0;
      assert placeholderStart >= 0;
      updatePlaceholder(placeholder, placeholderStart, length);
    }
  }

  private static Pair<Integer, Integer> getChangeForOffsets(int offset, int change, @NotNull final AnswerPlaceholder placeholder) {
    int placeholderStart = placeholder.getOffset();
    int placeholderEnd = placeholder.getEndOffset();
    int start = 0;
    int end = change;
    if (offset > placeholderEnd) {
      return Pair.create(0, 0);
    }

    if (offset < placeholderStart) {
      start = change;

      if (change < 0 && offset - change > placeholderStart) {  // delete part of placeholder start
        start = offset - placeholderStart;
      }
    }

    if (change < 0 && offset - change > placeholderEnd) {   // delete part of placeholder end
      end = offset - placeholderEnd;
    }

    return Pair.create(start, end);
  }

  protected void updatePlaceholder(@NotNull AnswerPlaceholder answerPlaceholder, int start, int length) {
    answerPlaceholder.setOffset(start);
    answerPlaceholder.setLength(length);
    if (updateYaml) {
      YamlFormatSynchronizer.saveItem(myTaskFile.getTask());
    }
  }
}

