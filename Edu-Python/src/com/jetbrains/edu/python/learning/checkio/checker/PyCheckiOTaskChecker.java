package com.jetbrains.edu.python.learning.checkio.checker;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NullUtils;
import com.jetbrains.edu.learning.EduNames;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.checker.CheckResult;
import com.jetbrains.edu.learning.checker.TaskChecker;
import com.jetbrains.edu.learning.checkio.connectors.CheckiOConnector;
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask;
import com.jetbrains.edu.learning.ui.taskDescription.TaskDescriptionToolWindow;
import org.jetbrains.annotations.NotNull;

public class PyCheckiOTaskChecker extends TaskChecker<EduTask> {
  private final CheckiOTestBrowserWindow myBrowserWindow;
  private final CheckiOTestResultPanel myTestResultPanel;

  public PyCheckiOTaskChecker(@NotNull EduTask task,
                              @NotNull Project project) {
    super(task, project);

    myBrowserWindow = new CheckiOTestBrowserWindow(project);
    myTestResultPanel = new CheckiOTestResultPanel(myBrowserWindow);
  }

  @Override
  public void onTaskSolved(@NotNull String message) {
    super.onTaskSolved(message);
  }

  @Override
  public void onTaskFailed(@NotNull String message) {
    super.onTaskFailed(message);
  }

  @NotNull
  @Override
  public CheckResult check() {
    final Editor selectedEditor = EduUtils.getSelectedEditor(project);
    if (selectedEditor == null) {
      return CheckResult.FAILED_TO_CHECK;
    }

    final String accessToken = CheckiOConnector.getAccessToken();
    final String taskIdAsString = String.valueOf(task.getId());
    final String interpreter = EduNames.CHECKIO_PYTHON_INTERPRETER;
    final String code = selectedEditor.getDocument().getText();

    if (NullUtils.notNull(accessToken, taskIdAsString, interpreter, code)) {
      showTestResultPanel();
      //noinspection ConstantConditions
      final CheckResult result = myBrowserWindow.checkOnBackground(accessToken, taskIdAsString, interpreter, code);
      return result;
    } else {
      return CheckResult.FAILED_TO_CHECK;
    }
  }

  private void showTestResultPanel() {
    final TaskDescriptionToolWindow toolWindow = EduUtils.getStudyToolWindow(project);
    if (toolWindow != null) {
      toolWindow.getContentPanel().add(CheckiOTestResultPanel.TEST_RESULTS_ID, myTestResultPanel);
      toolWindow.showPanelById(CheckiOTestResultPanel.TEST_RESULTS_ID);
    }
  }

  @Override
  public void clearState() {

  }
}
