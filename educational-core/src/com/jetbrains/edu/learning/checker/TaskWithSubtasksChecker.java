package com.jetbrains.edu.learning.checker;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.edu.learning.EduConfigurator;
import com.jetbrains.edu.learning.EduConfiguratorManager;
import com.jetbrains.edu.learning.EduNames;
import com.jetbrains.edu.learning.SubtaskUtils;
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder;
import com.jetbrains.edu.learning.courseFormat.TaskFile;
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask;
import com.jetbrains.edu.learning.courseFormat.tasks.TaskWithSubtasks;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class TaskWithSubtasksChecker extends TaskChecker<TaskWithSubtasks> {
  private TaskChecker<EduTask> myEduTaskChecker;
  public TaskWithSubtasksChecker(@NotNull TaskWithSubtasks task,
                                 @NotNull Project project) {
    super(task, project);
    EduConfigurator<?> configurator = EduConfiguratorManager.forLanguage(task.getLesson().getCourse().getLanguageById());
    if (configurator != null) {
      myEduTaskChecker = configurator.getTaskCheckerProvider().getEduTaskChecker(task, project);
    }
  }

  @NotNull
  @Override
  public CheckResult check() {
    if (myEduTaskChecker != null) {
      return myEduTaskChecker.check();
    }
    return super.check();
  }

  @Override
  public void onTaskSolved(@NotNull String message) {
    boolean hasMoreSubtasks = task.activeSubtaskNotLast();
    final int activeSubtaskIndex = task.getActiveSubtaskIndex();
    int visibleSubtaskIndex = activeSubtaskIndex + 1;
    ApplicationManager.getApplication().invokeLater(() -> {
      int subtaskSize = task.getLastSubtaskIndex() + 1;
      String resultMessage = !hasMoreSubtasks ? message : "Subtask " + visibleSubtaskIndex + "/" + subtaskSize + " solved";
      CheckUtils.showTestResultPopUp(resultMessage, MessageType.INFO.getPopupBackground(), project);
      if (hasMoreSubtasks) {
        int nextSubtaskIndex = activeSubtaskIndex + 1;
        SubtaskUtils.switchStep(project, task, nextSubtaskIndex);
        rememberAnswers(nextSubtaskIndex, task);
      }
    });
  }

  private void rememberAnswers(int nextSubtaskIndex, @NotNull TaskWithSubtasks task) {
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
