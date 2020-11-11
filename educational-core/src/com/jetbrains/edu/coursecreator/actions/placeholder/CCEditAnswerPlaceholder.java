package com.jetbrains.edu.coursecreator.actions.placeholder;

import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import com.jetbrains.edu.coursecreator.actions.placeholder.CCCreateAnswerPlaceholderDialog.DependencyInfo;
import com.jetbrains.edu.learning.EduState;
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder;
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholderDependency;
import com.jetbrains.edu.learning.messages.EduCoreBundle;
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer;
import org.jetbrains.annotations.NotNull;

public class CCEditAnswerPlaceholder extends CCAnswerPlaceholderAction {

  public CCEditAnswerPlaceholder() {
    super(() -> EduCoreBundle.message("label.edit"), () -> EduCoreBundle.message("action.edit.answer.placeholder.description"));
  }

  @Override
  protected void performAnswerPlaceholderAction(@NotNull Project project, @NotNull EduState state) {
    AnswerPlaceholder answerPlaceholder = state.getAnswerPlaceholder();
    if (answerPlaceholder == null) {
      return;
    }
    performEditPlaceholder(project, answerPlaceholder);
  }

  public void performEditPlaceholder(@NotNull Project project, @NotNull AnswerPlaceholder answerPlaceholder) {
    CCCreateAnswerPlaceholderDialog dlg = createDialog(project, answerPlaceholder);

    if (dlg.showAndGet()) {
      final String answerPlaceholderText = dlg.getPlaceholderText();
      answerPlaceholder.setPlaceholderText(answerPlaceholderText);
      final DependencyInfo dependencyInfo = dlg.getDependencyInfo();
      if (dependencyInfo != null) {
        answerPlaceholder.setPlaceholderDependency(
          AnswerPlaceholderDependency.create(answerPlaceholder, dependencyInfo.getDependencyPath(), dependencyInfo.isVisible()));
      }
      YamlFormatSynchronizer.saveItem(answerPlaceholder.getTaskFile().getTask());
    }
  }

  protected CCCreateAnswerPlaceholderDialog createDialog(@NotNull Project project, @NotNull AnswerPlaceholder answerPlaceholder) {
    return new CCCreateAnswerPlaceholderDialog(project, true, answerPlaceholder);
  }

  @Override
  protected void updatePresentation(@NotNull EduState eduState, @NotNull Presentation presentation) {
    presentation.setEnabledAndVisible(eduState.getAnswerPlaceholder() != null);
  }
}