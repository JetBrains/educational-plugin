package com.jetbrains.edu.coursecreator.actions.placeholder;

import com.google.common.collect.Streams;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.jetbrains.edu.coursecreator.stepik.StepikCourseChangeHandler;
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder;
import com.pty4j.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CCEditAnswerPlaceholder extends CCAnswerPlaceholderAction {

  public CCEditAnswerPlaceholder() {
    super("Edit", "Edit answer placeholder");
  }

  @Override
  protected void performAnswerPlaceholderAction(@NotNull CCState state) {
    final Project project = state.getProject();
    PsiFile file = state.getFile();
    final PsiDirectory taskDir = file.getContainingDirectory();
    final PsiDirectory lessonDir = taskDir.getParent();
    if (lessonDir == null) return;
    AnswerPlaceholder answerPlaceholder = state.getAnswerPlaceholder();
    if (answerPlaceholder == null) {
      return;
    }
    CCCreateAnswerPlaceholderDialog dlg = new CCCreateAnswerPlaceholderDialog(project, answerPlaceholder.getPlaceholderText(), answerPlaceholder.getHints());
    dlg.setTitle("Edit Answer Placeholder");
    if (dlg.showAndGet()) {
      final String answerPlaceholderText = dlg.getTaskText();
      if (isChanged(answerPlaceholder, dlg)) {
        StepikCourseChangeHandler.INSTANCE.changed(answerPlaceholder);
      }
      answerPlaceholder.setPlaceholderText(answerPlaceholderText);
      answerPlaceholder.setLength(answerPlaceholderText.length());
      answerPlaceholder.setHints(dlg.getHints());
    }
  }

  private static boolean isChanged(@NotNull AnswerPlaceholder answerPlaceholder, CCCreateAnswerPlaceholderDialog dialog) {
    if (!dialog.getTaskText().equals(answerPlaceholder.getPlaceholderText())) {
      return true;
    }

    List<String> newHints = dialog.getHints();
    List<String> oldHints = answerPlaceholder.getHints();
    if (newHints.size() != oldHints.size()) {
      return true;
    }

    return Streams
      .zip(oldHints.stream(), newHints.stream(), (oldHint, newHint) -> Pair.create(oldHint, newHint))
      .anyMatch(pair -> !pair.first.equals(pair.second));
  }

  @Override
  public void update(AnActionEvent e) {
    Presentation presentation = e.getPresentation();
    presentation.setEnabledAndVisible(false);
    CCState state = getState(e);
    if (state == null || state.getAnswerPlaceholder() == null) {
      return;
    }
    presentation.setEnabledAndVisible(true);
  }
}