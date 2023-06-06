package com.jetbrains.edu.learning.actions;

import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder;
import com.jetbrains.edu.learning.courseFormat.TaskFile;
import kotlin.collections.CollectionsKt;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PrevPlaceholderAction extends PlaceholderNavigationAction {
  @NonNls
  public static final String ACTION_ID = "Educational.PrevPlaceholder";

  @Nullable
  @Override
  protected AnswerPlaceholder getTargetPlaceholder(@NotNull final TaskFile taskFile, int offset) {
    final AnswerPlaceholder selectedAnswerPlaceholder = taskFile.getAnswerPlaceholder(offset);
    final List<AnswerPlaceholder> placeholders = taskFile.getAnswerPlaceholders();
    int endIndex = selectedAnswerPlaceholder != null ? selectedAnswerPlaceholder.getIndex() : placeholders.size();
    if (!indexIsValid(endIndex - 1, placeholders)) return null;

    for (AnswerPlaceholder placeholder : CollectionsKt.asReversed(placeholders.subList(0, endIndex))) {
      if (placeholder.getOffset() < offset && placeholder.isVisible()) {
        return placeholder;
      }
    }

    return null;
  }
}
