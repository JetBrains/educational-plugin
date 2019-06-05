package com.jetbrains.edu.coursecreator.actions.placeholder;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.jetbrains.edu.coursecreator.yaml.YamlFormatSynchronizer;
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder;
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholderDependency;
import com.jetbrains.edu.coursecreator.actions.placeholder.CCCreateAnswerPlaceholderDialog.DependencyInfo;
import org.jetbrains.annotations.NotNull;

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
    performEditPlaceholder(project, answerPlaceholder);
  }

  public void performEditPlaceholder(@NotNull Project project, @NotNull AnswerPlaceholder answerPlaceholder) {
    CCCreateAnswerPlaceholderDialog dlg = createDialog(project, answerPlaceholder);

    if (dlg.showAndGet()) {
      final String answerPlaceholderText = dlg.getTaskText();
      answerPlaceholder.setPlaceholderText(answerPlaceholderText);
      final DependencyInfo dependencyInfo = dlg.getDependencyInfo();
      if (dependencyInfo != null) {
        answerPlaceholder.setPlaceholderDependency(
          AnswerPlaceholderDependency.create(answerPlaceholder, dependencyInfo.getDependencyPath(), dependencyInfo.isVisible()));
      }
      YamlFormatSynchronizer.saveItem(answerPlaceholder.getTaskFile().getTask());
    }
  }

  public CCCreateAnswerPlaceholderDialog createDialog(@NotNull Project project, @NotNull AnswerPlaceholder answerPlaceholder) {
    return new CCCreateAnswerPlaceholderDialog(project, true, answerPlaceholder);
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