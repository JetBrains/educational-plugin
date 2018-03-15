package com.jetbrains.edu.coursecreator.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.command.undo.UndoManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.MapDataContext;
import com.jetbrains.edu.coursecreator.CCTestCase;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.courseFormat.TaskFile;
import org.jetbrains.annotations.NotNull;

public class CCTaskFileActionTest extends CCTestCase {
  public void testHideTaskFile() {
    VirtualFile virtualFile = configureByTaskFile("taskFile.txt");
    launchAction(virtualFile, new CCHideFromStudent());
    assertNull(EduUtils.getTaskFile(getProject(), virtualFile));
    UndoManager.getInstance(getProject()).undo(FileEditorManager.getInstance(getProject()).getSelectedEditor(virtualFile));
    TaskFile taskFile = EduUtils.getTaskFile(getProject(), virtualFile);
    assertNotNull(taskFile);
    checkPainters(taskFile);
  }

  public void testAddTaskFile() {
    VirtualFile virtualFile = copyFileToTask("nonTaskFile.txt");
    myFixture.configureFromExistingVirtualFile(virtualFile);
    launchAction(virtualFile, new CCAddAsTaskFile());
    TaskFile taskFile = EduUtils.getTaskFile(getProject(), virtualFile);
    assertNotNull(taskFile);
    FileEditor fileEditor = FileEditorManager.getInstance(getProject()).getSelectedEditor(virtualFile);
    UndoManager.getInstance(getProject()).undo(fileEditor);
    assertNull(EduUtils.getTaskFile(getProject(), virtualFile));
  }

  private void launchAction(@NotNull VirtualFile virtualFile, @NotNull AnAction action) {
    MapDataContext context = new MapDataContext();
    context.put(CommonDataKeys.VIRTUAL_FILE_ARRAY, new VirtualFile[]{virtualFile});
    context.put(CommonDataKeys.PROJECT, getProject());
    Presentation presentation = testAction(context, action);
    assertTrue(presentation.isEnabledAndVisible());
  }

  @Override
  protected String getBasePath() {
    return super.getBasePath() + "/actions/taskFileActions";
  }
}
