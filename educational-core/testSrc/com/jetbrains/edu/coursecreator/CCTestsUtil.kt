package com.jetbrains.edu.coursecreator;

import com.jetbrains.edu.learning.PlaceholderPainter;
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder;
import com.jetbrains.edu.learning.courseFormat.TaskFile;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class CCTestsUtil {

  private CCTestsUtil() {
  }

  public static String getPlaceholderPresentation(AnswerPlaceholder placeholder) {
    return "offset=" + placeholder.getOffset() +
           " length=" + placeholder.getLength() +
           " possibleAnswer=" + placeholder.getPossibleAnswer() +
           " placeholderText=" + placeholder.getPlaceholderText();
  }

  public static void checkPainters(@NotNull AnswerPlaceholder placeholder) {
    final Set<AnswerPlaceholder> paintedPlaceholders = PlaceholderPainter.getPaintedPlaceholder();
    if (paintedPlaceholders.contains(placeholder)) return;
    for (AnswerPlaceholder paintedPlaceholder : paintedPlaceholders) {
      if (paintedPlaceholder.getOffset() == placeholder.getOffset() &&
          paintedPlaceholder.getLength() == placeholder.getLength()) {
        return;
      }
    }
    throw new AssertionError("No highlighter for placeholder: " + CCTestsUtil.getPlaceholderPresentation(placeholder));
  }

  public static void checkPainters(@NotNull TaskFile taskFile) {
    final Set<AnswerPlaceholder> paintedPlaceholders = PlaceholderPainter.getPaintedPlaceholder();

    for (AnswerPlaceholder answerPlaceholder : taskFile.getAnswerPlaceholders()) {
      if (!paintedPlaceholders.contains(answerPlaceholder)) {
        throw new AssertionError("No highlighter for placeholder: " + CCTestsUtil.getPlaceholderPresentation(answerPlaceholder));
      }
    }
  }
}
