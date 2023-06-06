package com.jetbrains.edu.learning.handlers;

import com.intellij.codeInsight.hint.HintManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ReadOnlyFragmentModificationException;
import com.intellij.openapi.editor.actionSystem.ReadonlyFragmentModificationHandler;
import com.jetbrains.edu.learning.messages.EduCoreBundle;
import org.jetbrains.annotations.NotNull;

public class AnswerPlaceholderDeleteHandler implements ReadonlyFragmentModificationHandler {

  private final Editor myEditor;

  public AnswerPlaceholderDeleteHandler(@NotNull final Editor editor) {
    myEditor = editor;
  }

  @Override
  public void handle(ReadOnlyFragmentModificationException e) {
    if (myEditor.isDisposed()) return;
    HintManager.getInstance().showErrorHint(myEditor, EduCoreBundle.message("notification.text.error.hint.placeholder.delete"));
  }
}
