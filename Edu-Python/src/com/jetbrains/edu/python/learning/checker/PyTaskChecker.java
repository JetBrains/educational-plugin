package com.jetbrains.edu.python.learning.checker;

import com.intellij.execution.ExecutionException;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.edu.learning.EduState;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.checker.CheckResult;
import com.jetbrains.edu.learning.checker.CheckUtils;
import com.jetbrains.edu.learning.checker.TaskChecker;
import com.jetbrains.edu.learning.checker.TestsOutputParser;
import com.jetbrains.edu.learning.courseFormat.CheckStatus;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.TaskFile;
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.editor.EduEditor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class PyTaskChecker extends TaskChecker<EduTask> {
  private static final Logger LOG = Logger.getInstance(PyTaskChecker.class);

  public PyTaskChecker(@NotNull EduTask task, @NotNull Project project) {
    super(task, project);
  }

  @NotNull
  @Override
  public CheckResult check() {
    VirtualFile taskDir = task.getTaskDir(project);
    if (taskDir == null) {
      LOG.info("taskDir is null for task " + task.getName());
      return new CheckResult(CheckStatus.Unchecked, "Task is broken");
    }

    if (!task.isValid(project)) {
      return new CheckResult(CheckStatus.Unchecked,
              EduEditor.BROKEN_SOLUTION_ERROR_TEXT_START + EduEditor.ACTION_TEXT + EduEditor.BROKEN_SOLUTION_ERROR_TEXT_END);
    }
    CountDownLatch latch = new CountDownLatch(1);
    ApplicationManager.getApplication()
      .invokeLater(() -> ApplicationManager.getApplication().runWriteAction(() -> {
        CheckUtils.flushWindows(task, taskDir);
        latch.countDown();
      }));
    final PyTestRunner testRunner = new PyTestRunner(task, taskDir);
    try {
      final VirtualFile fileToCheck = getTaskVirtualFile(task, taskDir);
      if (fileToCheck != null) {
        //otherwise answer placeholders might have been not flushed yet
        latch.await();
        Process testProcess = testRunner.createCheckProcess(project, fileToCheck.getPath());
        TestsOutputParser.TestsOutput output =
          CheckUtils
            .getTestOutput(testProcess, testRunner.getCommandLine().getCommandLineString(), task.getLesson().getCourse().isAdaptive());
        return new CheckResult(output.isSuccess() ? CheckStatus.Solved : CheckStatus.Failed, output.getMessage());
      }
    }
    catch (ExecutionException | InterruptedException e) {
      LOG.error(e);
    }
    return CheckResult.FAILED_TO_CHECK;
  }

  @Override
  public void clearState() {
    ApplicationManager.getApplication().invokeLater(() -> {
      CheckUtils.drawAllPlaceholders(project, task);
      VirtualFile taskDir = task.getTaskDir(project);
      if (taskDir != null) {
        EduUtils.deleteWindowDescriptions(task, taskDir);
      }
    });
  }

  @Override
  public void onTaskFailed(@NotNull String message) {
    super.onTaskFailed(message);
    ApplicationManager.getApplication().invokeLater(() -> {
      VirtualFile taskDir = task.getTaskDir(project);
      if (taskDir == null) return;
      for (Map.Entry<String, TaskFile> entry : task.getTaskFiles().entrySet()) {
        final TaskFile taskFile = entry.getValue();
        if (taskFile.getActivePlaceholders().size() < 2) {
          continue;
        }
        final Course course = task.getLesson().getCourse();
        if (course != null && course.isStudy()) {
          CommandProcessor.getInstance().runUndoTransparentAction(
            () -> ApplicationManager.getApplication().runWriteAction(
              () -> PySmartChecker.runSmartTestProcess(taskDir, new PyTestRunner(task, taskDir), taskFile, project)));
        }
      }
      CheckUtils.navigateToFailedPlaceholder(new EduState(EduUtils.getSelectedStudyEditor(project)), task, taskDir, project);
    });
  }

  @Nullable
  private static VirtualFile getTaskVirtualFile(@NotNull final Task task,
                                                @NotNull final VirtualFile taskDir) {
    VirtualFile firstFile = null;
    for (Map.Entry<String, TaskFile> entry : task.getTaskFiles().entrySet()) {
      TaskFile taskFile = entry.getValue();
      VirtualFile file = EduUtils.findTaskFileInDir(taskFile, taskDir);
      if (file == null) continue;
      if (firstFile == null) {
        firstFile = file;
      }
      if (!taskFile.getActivePlaceholders().isEmpty()) return file;
    }
    return firstFile;
  }
}
