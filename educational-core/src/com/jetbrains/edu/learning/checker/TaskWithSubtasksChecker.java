package com.jetbrains.edu.learning.checker;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.edu.learning.EduNames;
import com.jetbrains.edu.learning.SubtaskUtils;
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder;
import com.jetbrains.edu.learning.courseFormat.TaskFile;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.courseFormat.tasks.TaskWithSubtasks;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class TaskWithSubtasksChecker extends TaskChecker {
  @Override
  public boolean isAccepted(@NotNull Task task) {
    return task instanceof TaskWithSubtasks;
  }

  @NotNull
  @Override
  public CheckResult check(@NotNull Task task, @NotNull Project project) {
    TaskChecker checker = CollectionsKt.firstOrNull(TaskCheckerManager.getCheckers(task), new Function1<TaskChecker, Boolean>() {
      @Override
      public Boolean invoke(TaskChecker eduTaskChecker) {
        return !(eduTaskChecker instanceof TaskWithSubtasksChecker);
      }
    });
    if (checker != null) {
      return checker.check(task, project);
    }
    return super.check(task, project);
  }

  @NotNull
  @Override
  public CheckResult checkOnRemote(@NotNull Task task, @NotNull Project project) {
    TaskChecker checker = CollectionsKt.firstOrNull(TaskCheckerManager.getCheckers(task), new Function1<TaskChecker, Boolean>() {
      @Override
      public Boolean invoke(TaskChecker eduTaskChecker) {
        return !(eduTaskChecker instanceof TaskWithSubtasksChecker);
      }
    });
    if (checker != null) {
      return checker.checkOnRemote(task, project);
    }
    return super.checkOnRemote(task, project);
  }

  @Override
  public void onTaskSolved(@NotNull Task task, @NotNull Project project, @NotNull String message) {
    assert task instanceof TaskWithSubtasks;
    TaskWithSubtasks myTask = (TaskWithSubtasks) task;
    boolean hasMoreSubtasks = myTask.activeSubtaskNotLast();
    final int activeSubtaskIndex = myTask.getActiveSubtaskIndex();
    int visibleSubtaskIndex = activeSubtaskIndex + 1;
    ApplicationManager.getApplication().invokeLater(() -> {
      int subtaskSize = myTask.getLastSubtaskIndex() + 1;
      String resultMessage = !hasMoreSubtasks ? message : "Subtask " + visibleSubtaskIndex + "/" + subtaskSize + " solved";
      CheckUtils.showTestResultPopUp(resultMessage, MessageType.INFO.getPopupBackground(), project);
      if (hasMoreSubtasks) {
        int nextSubtaskIndex = activeSubtaskIndex + 1;
        SubtaskUtils.switchStep(project, myTask, nextSubtaskIndex);
        rememberAnswers(nextSubtaskIndex, myTask, project);
      }
    });
  }

  private void rememberAnswers(int nextSubtaskIndex, @NotNull Task task, @NotNull Project project) {
    VirtualFile taskDir = task.getTaskDir(project);
    if (taskDir == null) {
      return;
    }
    VirtualFile srcDir = taskDir.findChild(EduNames.SRC);
    if (srcDir != null) {
      taskDir = srcDir;
    }
    for (Map.Entry<String, TaskFile> entry : task.getTaskFiles().entrySet()) {
      TaskFile taskFile = entry.getValue();
      VirtualFile virtualFile = taskDir.findFileByRelativePath(entry.getKey());
      if (virtualFile == null) {
        continue;
      }
      Document document = FileDocumentManager.getInstance().getDocument(virtualFile);
      if (document == null) {
        continue;
      }
      for (AnswerPlaceholder placeholder : taskFile.getActivePlaceholders()) {
        if (placeholder.getSubtaskInfos().containsKey(nextSubtaskIndex - 1)) {
          int offset = placeholder.getOffset();
          String answer = document.getText(TextRange.create(offset, offset + placeholder.getRealLength()));
          placeholder.getSubtaskInfos().get(nextSubtaskIndex - 1).setAnswer(answer);
        }
      }
    }
  }
}
