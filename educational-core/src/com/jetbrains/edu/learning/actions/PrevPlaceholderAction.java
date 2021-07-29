package com.jetbrains.edu.learning.actions;

import com.intellij.icons.AllIcons;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder;
import com.jetbrains.edu.learning.courseFormat.TaskFile;
import com.jetbrains.edu.learning.messages.EduCoreBundle;
import kotlin.collections.CollectionsKt;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PrevPlaceholderAction extends PlaceholderNavigationAction {
  @NonNls
  public static final String ACTION_ID = "Educational.PrevPlaceholder";

  public PrevPlaceholderAction() {
    super(EduCoreBundle.lazyMessage("action.previous.placeholder.text"),
          EduCoreBundle.lazyMessage("action.previous.placeholder.description"),
          AllIcons.Actions.Back);
  }

  @Nullable
  @Override
  protected AnswerPlaceholder getTargetPlaceholder(@NotNull final TaskFile taskFile, int offset) {
    final AnswerPlaceholder selectedAnswerPlaceholder = taskFile.getAnswerPlaceholder(offset);
    final List<AnswerPlaceholder> placeholders = taskFile.getAnswerPlaceholders();
    int endIndex = selectedAnswerPlaceholder != null ? selectedAnswerPlaceholder.getIndex() : placeholders.size();
    if (!EduUtils.indexIsValid(endIndex - 1, placeholders)) return null;

    for (AnswerPlaceholder placeholder : CollectionsKt.asReversed(placeholders.subList(0, endIndex))) {
      if (placeholder.getOffset() < offset) {
        return placeholder;
      }
    }

    return null;
  }
}
