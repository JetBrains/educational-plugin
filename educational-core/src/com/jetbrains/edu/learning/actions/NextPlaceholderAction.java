package com.jetbrains.edu.learning.actions;

import com.intellij.icons.AllIcons;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder;
import com.jetbrains.edu.learning.courseFormat.TaskFile;
import com.jetbrains.edu.learning.messages.EduCoreBundle;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * move caret to next answer placeholder
 */
public class NextPlaceholderAction extends PlaceholderNavigationAction {
  @NonNls
  public static final String ACTION_ID = "Educational.NextPlaceholder";

  public NextPlaceholderAction() {
    super(EduCoreBundle.lazyMessage("action.next.placeholder.text"),
          EduCoreBundle.lazyMessage("action.next.placeholder.description"),
          AllIcons.Actions.Forward);
  }

  @Nullable
  @Override
  protected AnswerPlaceholder getTargetPlaceholder(@NotNull final TaskFile taskFile, int offset) {
    final AnswerPlaceholder selectedAnswerPlaceholder = taskFile.getAnswerPlaceholder(offset);
    final List<AnswerPlaceholder> placeholders = taskFile.getAnswerPlaceholders();
    int startIndex = selectedAnswerPlaceholder != null ? selectedAnswerPlaceholder.getIndex() + 1 : 0;
    if (!EduUtils.indexIsValid(startIndex, placeholders)) return null;

    for (AnswerPlaceholder placeholder : placeholders.subList(startIndex, placeholders.size())) {
      if (placeholder.getOffset() > offset) {
        return placeholder;
      }
    }

    return null;
  }
}
