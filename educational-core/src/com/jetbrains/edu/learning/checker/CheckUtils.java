package com.jetbrains.edu.learning.checker;

import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.IdeFocusManager;
import com.jetbrains.edu.learning.EduState;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseFormat.TaskFile;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.editor.EduSingleFileEditor;
import com.jetbrains.edu.learning.navigation.NavigationUtils;
import kotlin.collections.CollectionsKt;
import kotlin.text.StringsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;
import java.util.Map;

public class CheckUtils {
  public static final String STUDY_PREFIX = "#educational_plugin";
  public static final String CONGRATULATIONS = "Congratulations!";
  public static final String TEST_OK = "test OK";
  public static final String TEST_FAILED = "FAILED + ";
  public static final String CONGRATS_MESSAGE = "CONGRATS_MESSAGE ";
  
  public static final List<String> COMPILATION_ERRORS = CollectionsKt.listOf("Compilation failed", "Compilation error");
  public static final String COMPILATION_FAILED_MESSAGE = "Compilation failed";
  public static final String NOT_RUNNABLE_MESSAGE = "Solution isn't runnable";
  public static final String LOGIN_NEEDED_MESSAGE = "Please, login to Stepik to check the task";
  public static final String FAILED_TO_CHECK_MESSAGE = "Failed to launch checking";

  private CheckUtils() {
  }

  public static void navigateToFailedPlaceholder(@NotNull final EduState eduState,
                                                 @NotNull final Task task,
                                                 @NotNull final VirtualFile taskDir,
                                                 @NotNull final Project project) {
    TaskFile selectedTaskFile = eduState.getTaskFile();
    if (selectedTaskFile == null) return;
    Editor editor = eduState.getEditor();
    TaskFile taskFileToNavigate = selectedTaskFile;
    VirtualFile fileToNavigate = eduState.getVirtualFile();
    final StudyTaskManager studyTaskManager = StudyTaskManager.getInstance(project);
    if (!studyTaskManager.hasFailedAnswerPlaceholders(selectedTaskFile)) {
      for (Map.Entry<String, TaskFile> entry : task.getTaskFiles().entrySet()) {
        TaskFile taskFile = entry.getValue();
        if (studyTaskManager.hasFailedAnswerPlaceholders(taskFile)) {
          taskFileToNavigate = taskFile;
          VirtualFile virtualFile = EduUtils.findTaskFileInDir(taskFile, taskDir);
          if (virtualFile == null) {
            continue;
          }
          FileEditor fileEditor = FileEditorManager.getInstance(project).getSelectedEditor(virtualFile);
          if (fileEditor instanceof EduSingleFileEditor) {
            EduSingleFileEditor eduEditor = (EduSingleFileEditor)fileEditor;
            editor = eduEditor.getEditor();
          }
          fileToNavigate = virtualFile;
          break;
        }
      }
    }
    if (fileToNavigate != null) {
      FileEditorManager.getInstance(project).openFile(fileToNavigate, true);
    }
    if (editor == null) {
      return;
    }
    final Editor editorToNavigate = editor;
    ApplicationManager.getApplication().invokeLater(
      () -> IdeFocusManager.getInstance(project).requestFocus(editorToNavigate.getContentComponent(), true));

    NavigationUtils.navigateToFirstFailedAnswerPlaceholder(editor, taskFileToNavigate);
  }

  public static void flushWindows(@NotNull final Task task, @NotNull final VirtualFile taskDir) {
    for (Map.Entry<String, TaskFile> entry : task.getTaskFiles().entrySet()) {
      TaskFile taskFile = entry.getValue();
      VirtualFile virtualFile = EduUtils.findTaskFileInDir(taskFile, taskDir);
      if (virtualFile == null) {
        continue;
      }
      EduUtils.flushWindows(taskFile, virtualFile);
    }
  }

  @Nullable
  public static RunnerAndConfigurationSettings createDefaultRunConfiguration(@NotNull Project project) {
    return ApplicationManager.getApplication().runReadAction((Computable<RunnerAndConfigurationSettings>) () -> {
      Editor editor = EduUtils.getSelectedEditor(project);
      if (editor == null) return null;
      JComponent editorComponent = editor.getComponent();
      DataContext dataContext = DataManager.getInstance().getDataContext(editorComponent);
      return ConfigurationContext.getFromContext(dataContext).getConfiguration();
    });
  }

  public static boolean hasCompilationErrors(ProcessOutput processOutput) {
    for (String error : COMPILATION_ERRORS) {
      if (processOutput.getStderr().contains(error)) return true;
    }
    return false;
  }

  public static String postProcessOutput(@NotNull String output) {
    return StringsKt.removeSuffix(output.replace(System.getProperty("line.separator"), "\n"), "\n");
  }
}