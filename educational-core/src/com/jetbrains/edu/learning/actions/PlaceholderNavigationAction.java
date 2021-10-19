package com.jetbrains.edu.learning.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.OpenApiExtKt;
import com.jetbrains.edu.learning.VirtualFileExt;
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder;
import com.jetbrains.edu.learning.courseFormat.TaskFile;
import com.jetbrains.edu.learning.navigation.NavigationUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

abstract public class PlaceholderNavigationAction extends DumbAwareAction {

  private void navigateToPlaceholder(@NotNull final Project project) {
    final Editor selectedEditor = OpenApiExtKt.getSelectedEditor(project);
    if (selectedEditor != null) {
      final FileDocumentManager fileDocumentManager = FileDocumentManager.getInstance();
      final VirtualFile openedFile = fileDocumentManager.getFile(selectedEditor.getDocument());
      if (openedFile != null) {
        final TaskFile selectedTaskFile = VirtualFileExt.getTaskFile(openedFile, project);
        if (selectedTaskFile != null) {
          final int offset = selectedEditor.getCaretModel().getOffset();
          final AnswerPlaceholder targetPlaceholder = getTargetPlaceholder(selectedTaskFile, offset);
          if (targetPlaceholder == null) {
            return;
          }
          NavigationUtils.navigateToAnswerPlaceholder(selectedEditor, targetPlaceholder);
        }
      }
    }
  }

  @Nullable
  protected abstract AnswerPlaceholder getTargetPlaceholder(@NotNull final TaskFile taskFile, int offset);

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    final Project project = e.getProject();
    if (project == null) {
      return;
    }
    navigateToPlaceholder(project);
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    EduUtils.updateAction(e);
  }
}
