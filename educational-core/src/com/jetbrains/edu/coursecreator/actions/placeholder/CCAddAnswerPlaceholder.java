package com.jetbrains.edu.coursecreator.actions.placeholder;

import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.util.DocumentUtil;
import com.jetbrains.edu.coursecreator.CCUtils;
import com.jetbrains.edu.coursecreator.actions.placeholder.CCCreateAnswerPlaceholderDialog.DependencyInfo;
import com.jetbrains.edu.learning.EduState;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.PlaceholderPainter;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.configuration.EduConfigurator;
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder;
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholderDependency;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.TaskFile;
import com.jetbrains.edu.learning.courseFormat.ext.CourseExt;
import com.jetbrains.edu.learning.messages.EduCoreBundle;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CCAddAnswerPlaceholder extends CCAnswerPlaceholderAction {

  public CCAddAnswerPlaceholder() {
    super(EduCoreBundle.lazyMessage("action.add.answer.placeholder.text"),
          EduCoreBundle.lazyMessage("action.add.answer.placeholder.description"));
  }


  private static boolean arePlaceholdersIntersect(@NotNull final TaskFile taskFile, int start, int end) {
    List<AnswerPlaceholder> answerPlaceholders = taskFile.getAnswerPlaceholders();
    for (AnswerPlaceholder existingAnswerPlaceholder : answerPlaceholders) {
      int twStart = existingAnswerPlaceholder.getOffset();
      int twEnd = existingAnswerPlaceholder.getPossibleAnswer().length() + twStart;
      if ((start >= twStart && start < twEnd) || (end > twStart && end <= twEnd) ||
          (twStart >= start && twStart < end) || (twEnd > start && twEnd <= end)) {
        return true;
      }
    }
    return false;
  }

  private void addPlaceholder(@NotNull Project project, @NotNull EduState state) {
    Editor editor = state.getEditor();
    Document document = editor.getDocument();
    FileDocumentManager.getInstance().saveDocument(document);
    final SelectionModel model = editor.getSelectionModel();
    final int offset = model.hasSelection() ? model.getSelectionStart() : editor.getCaretModel().getOffset();
    TaskFile taskFile = state.getTaskFile();
    final AnswerPlaceholder answerPlaceholder = new AnswerPlaceholder();
    int index = taskFile.getAnswerPlaceholders().size();
    answerPlaceholder.setIndex(index);
    answerPlaceholder.setTaskFile(taskFile);
    taskFile.sortAnswerPlaceholders();
    answerPlaceholder.setOffset(offset);

    @NonNls String defaultPlaceholderText = getDefaultPlaceholderText(project);
    answerPlaceholder.setPlaceholderText(defaultPlaceholderText);
    CCCreateAnswerPlaceholderDialog dlg = createDialog(project, answerPlaceholder);
    if (!dlg.showAndGet()) {
      return;
    }
    String answerPlaceholderText = dlg.getPlaceholderText();
    String possibleAnswer = model.hasSelection() ? model.getSelectedText() : defaultPlaceholderText;
    if (possibleAnswer == null) {
      possibleAnswer = defaultPlaceholderText;
    }
    answerPlaceholder.setPlaceholderText(answerPlaceholderText);
    answerPlaceholder.setLength(possibleAnswer.length());
    final DependencyInfo dependencyInfo = dlg.getDependencyInfo();
    if (dependencyInfo != null) {
      answerPlaceholder.setPlaceholderDependency(
        AnswerPlaceholderDependency.create(answerPlaceholder, dependencyInfo.getDependencyPath(), dependencyInfo.isVisible()));
    }

    if (!model.hasSelection()) {
      DocumentUtil.writeInRunUndoTransparentAction(() -> document.insertString(offset, defaultPlaceholderText));
    }

    AddAction action = new AddAction(project, answerPlaceholder, taskFile, editor);
    EduUtils.runUndoableAction(project, EduCoreBundle.message("action.add.answer.placeholder.text"), action);
  }

  @NotNull
  private static String getDefaultPlaceholderText(@NotNull final Project project) {
    final Course course = StudyTaskManager.getInstance(project).getCourse();
    if (course == null) {
      return CCUtils.DEFAULT_PLACEHOLDER_TEXT;
    }
    EduConfigurator<?> configurator = CourseExt.getConfigurator(course);
    if (configurator == null) {
      return CCUtils.DEFAULT_PLACEHOLDER_TEXT;
    }
    return configurator.getDefaultPlaceholderText();
  }

  static class AddAction extends TaskFileUndoableAction {
    private final AnswerPlaceholder myPlaceholder;

    public AddAction(@NotNull Project project, @NotNull AnswerPlaceholder placeholder,
                     @NotNull TaskFile taskFile, @NotNull Editor editor) {
      super(project, taskFile, editor);
      myPlaceholder = placeholder;
    }

    @Override
    public boolean performUndo() {
      final List<AnswerPlaceholder> answerPlaceholders = getTaskFile().getAnswerPlaceholders();
      if (answerPlaceholders.contains(myPlaceholder)) {
        answerPlaceholders.remove(myPlaceholder);
        PlaceholderPainter.hidePlaceholder(myPlaceholder);
        return true;
      }

      return false;
    }

    @Override
    public void performRedo() {
      getTaskFile().addAnswerPlaceholder(myPlaceholder);
      PlaceholderPainter.showPlaceholder(getProject(), myPlaceholder);
    }
  }

  @Override
  protected void performAnswerPlaceholderAction(@NotNull Project project, @NotNull EduState state) {
    addPlaceholder(project, state);
  }

  @Override
  protected void updatePresentation(@NotNull EduState eduState, @NotNull Presentation presentation) {
    presentation.setVisible(true);
    if (canAddPlaceholder(eduState)) {
      presentation.setEnabled(true);
    }
  }

  private static boolean canAddPlaceholder(@NotNull EduState state) {
    Editor editor = state.getEditor();
    SelectionModel selectionModel = editor.getSelectionModel();
    TaskFile taskFile = state.getTaskFile();
    if (selectionModel.hasSelection()) {
      int start = selectionModel.getSelectionStart();
      int end = selectionModel.getSelectionEnd();
      return !arePlaceholdersIntersect(taskFile, start, end);
    }
    int offset = editor.getCaretModel().getOffset();
    return taskFile.getAnswerPlaceholder(offset) == null;
  }

  protected CCCreateAnswerPlaceholderDialog createDialog(Project project, AnswerPlaceholder answerPlaceholder) {
    return new CCCreateAnswerPlaceholderDialog(project, false, answerPlaceholder);
  }
}
