package com.jetbrains.edu.learning.actions;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.ui.popup.PopupPositionManager;
import com.jetbrains.edu.learning.EduState;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.TaskFile;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.statistics.EduUsagesCollector;
import com.jetbrains.edu.learning.ui.AnswerPlaceholderHint;
import com.jetbrains.edu.learning.ui.taskDescription.TaskDescriptionToolWindow;
import icons.EducationalCoreIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.List;

public class ShowHintAction extends DumbAwareActionWithShortcut {
  public static final String ACTION_ID = "Educational.ShowHint";
  public static final String SHORTCUT = "ctrl pressed 7";

  public ShowHintAction() {
    super("Show hint", "Show hint", EducationalCoreIcons.ShowHint);
  }

  public void actionPerformed(@NotNull AnActionEvent e) {
    final Project project = e.getProject();
    if (project == null) {
      return;
    }
    showHint(project);
  }

  public void showHint(Project project) {
    Course course = StudyTaskManager.getInstance(project).getCourse();
    if (course == null) {
      return;
    }
    EduState eduState = new EduState(EduUtils.getSelectedStudyEditor(project));
    if (!eduState.isValid()) {
      return;
    }
    PsiFile file = PsiManager.getInstance(project).findFile(eduState.getVirtualFile());
    final Editor editor = eduState.getEditor();
    int offset = editor.getCaretModel().getOffset();
    AnswerPlaceholder answerPlaceholder = eduState.getTaskFile().getAnswerPlaceholder(offset);
    if (file == null) {
      return;
    }
    EduUsagesCollector.hintShown();

    final TaskDescriptionToolWindow hintComponent = getHint(project, answerPlaceholder).getTaskDescriptionToolWindow();
    hintComponent.setPreferredSize(new Dimension(400, 150));
    showHintPopUp(project, eduState, editor, hintComponent);
  }

  @NotNull
  protected AnswerPlaceholderHint getHint(Project project, AnswerPlaceholder answerPlaceholder) {
    return new AnswerPlaceholderHint(answerPlaceholder, project);
  }

  private static void showHintPopUp(Project project, EduState eduState, Editor editor, TaskDescriptionToolWindow hintComponent) {
    final JBPopup popup =
      JBPopupFactory.getInstance().createComponentPopupBuilder(hintComponent, hintComponent)
        .setDimensionServiceKey(project, "StudyHint", false)
        .setResizable(true)
        .setMovable(true)
        .setRequestFocus(true)
        .setTitle(eduState.getTask().getName())
        .createPopup();
    Disposer.register(popup, hintComponent);

    final Component focusOwner = IdeFocusManager.getInstance(project).getFocusOwner();
    DataContext dataContext = DataManager.getInstance().getDataContext(focusOwner);
    PopupPositionManager.positionPopupInBestPosition(popup, editor, dataContext);
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    final Project project = e.getProject();
    if (project != null) {
      final Course course = StudyTaskManager.getInstance(project).getCourse();
      Presentation presentation = e.getPresentation();
      if (course != null && course.isAdaptive()) {
        presentation.setEnabled(false);
        return;
      }
      EduUtils.updateAction(e);
      if (!presentation.isEnabled()) {
        return;
      }
      if (EduUtils.isStudentProject(project)) {
        presentation.setEnabledAndVisible(hasHints(project));
      }
    }
    
  }

  private boolean hasHints(@NotNull Project project) {
    Task currentTask = EduUtils.getCurrentTask(project);
    if (currentTask == null) {
      return false;
    }
    for (TaskFile taskFile : currentTask.getTaskFiles().values()) {
      List<AnswerPlaceholder> placeholders = taskFile.getActivePlaceholders();
      if (!placeholders.isEmpty()) {
        for (AnswerPlaceholder placeholder : placeholders) {
          List<String> hints = placeholder.getHints();
          if (!hints.isEmpty()) {
            if (hints.size() != 1 || !hints.get(0).isEmpty()) {
              return true;
            }
          }
        }
      }
    }
    return false;
  }

  @NotNull
  @Override
  public String getActionId() {
    return ACTION_ID;
  }

  @Nullable
  @Override
  public String[] getShortcuts() {
    return new String[]{SHORTCUT};
  }
}
