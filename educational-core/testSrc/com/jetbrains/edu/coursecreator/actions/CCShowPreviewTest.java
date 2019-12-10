package com.jetbrains.edu.coursecreator.actions;

import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiManager;
import com.intellij.testFramework.MapDataContext;
import com.jetbrains.edu.coursecreator.CCTestCase;
import com.jetbrains.edu.coursecreator.CCTestsUtil;
import com.jetbrains.edu.coursecreator.actions.taskFile.CCShowPreview;
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Deprecated. Please write new tests in
 * @see com.jetbrains.edu.coursecreator.actions.CCCreateTaskFilePreviewTest
 */
public class CCShowPreviewTest extends CCTestCase {
  public void testOnePlaceholder() {
    doTest("test");
  }

  public void testSeveralPlaceholders() {
    doTest("several");
  }

  public void testNestedTaskFile() {
    VirtualFile file = configureByTaskFile("src/Task.txt");
    testAction(createDataContext(file), new CCShowPreview());
    Editor editor = EditorFactory.getInstance().getAllEditors()[1];
    try {
      Document document = editor.getDocument();
      VirtualFile virtualFile = FileDocumentManager.getInstance().getFile(document);
      assert virtualFile != null;
      assertEquals("Task.txt", virtualFile.getName());
    }
    finally {
      EditorFactory.getInstance().releaseEditor(editor);
    }
  }

  private void doTest(String name) {
    VirtualFile file = configureByTaskFile(name + CCTestsUtil.BEFORE_POSTFIX);
    Presentation presentation = testAction(createDataContext(file), new CCShowPreview());
    assertTrue(presentation.isEnabledAndVisible());
    Editor editor = EditorFactory.getInstance().getAllEditors()[1];
    try {
      Pair<Document, List<AnswerPlaceholder>> pair = getPlaceholders(name + CCTestsUtil.AFTER_POSTFIX);
      assertEquals("Files don't match", pair.getFirst().getText(), editor.getDocument().getText());
      for (AnswerPlaceholder placeholder : pair.getSecond()) {
        checkPainters(placeholder);
      }
    }
    finally {
      EditorFactory.getInstance().releaseEditor(editor);
    }
  }

  @Override
  protected String getBasePath() {
    return super.getBasePath() + "/actions/preview";
  }

  private DataContext createDataContext(@NotNull VirtualFile file) {
    MapDataContext context = new MapDataContext();
    context.put(CommonDataKeys.PSI_FILE, PsiManager.getInstance(getProject()).findFile(file));
    context.put(CommonDataKeys.PROJECT, getProject());
    context.put(LangDataKeys.MODULE, myFixture.getModule());
    return context;
  }
}
