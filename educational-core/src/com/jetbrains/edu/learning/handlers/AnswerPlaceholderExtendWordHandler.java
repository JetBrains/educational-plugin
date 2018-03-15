package com.jetbrains.edu.learning.handlers;

import com.intellij.codeInsight.editorActions.ExtendWordSelectionHandler;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.containers.ContainerUtil;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder;
import com.jetbrains.edu.learning.courseFormat.TaskFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class AnswerPlaceholderExtendWordHandler implements ExtendWordSelectionHandler {

  @Nullable
  private static AnswerPlaceholder getAnswerPlaceholder(PsiElement e, int offset) {
    PsiFile file = e.getContainingFile();
    if (file == null) {
      return null;
    }
    VirtualFile virtualFile = file.getVirtualFile();
    if (virtualFile == null) {
      return null;
    }
    TaskFile taskFile = EduUtils.getTaskFile(e.getProject(), virtualFile);
    if (taskFile == null) {
      return null;
    }
    Document document = FileDocumentManager.getInstance().getDocument(virtualFile);
    if (document == null) {
      return null;
    }
    Editor editor = FileEditorManager.getInstance(e.getProject()).getSelectedTextEditor();
    return editor == null ? null : taskFile.getAnswerPlaceholder(offset);
  }

  @Override
  public boolean canSelect(@NotNull PsiElement element) {
    Editor editor = FileEditorManager.getInstance(element.getProject()).getSelectedTextEditor();
    if (editor == null) {
      return false;
    }
    return getAnswerPlaceholder(element, editor.getCaretModel().getOffset()) != null;
  }

  @Override
  public List<TextRange> select(@NotNull PsiElement element, @NotNull CharSequence editorText, int cursorOffset, @NotNull Editor editor) {
    AnswerPlaceholder placeholder = getAnswerPlaceholder(element, editor.getCaretModel().getOffset());
    if (placeholder == null) return ContainerUtil.newArrayList();
    final Pair<Integer, Integer> offsets = EduUtils.getPlaceholderOffsets(placeholder);
    return Collections.singletonList(new TextRange(offsets.getFirst(), offsets.getSecond()));
  }
}
