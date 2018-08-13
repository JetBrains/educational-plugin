package com.jetbrains.edu.python.learning.checkio.checker;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ex.ApplicationUtil;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.checker.CheckResult;
import com.jetbrains.edu.learning.checker.TaskChecker;
import com.jetbrains.edu.learning.checkio.api.exceptions.ApiException;
import com.jetbrains.edu.learning.checkio.api.exceptions.NetworkException;
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOCourse;
import com.jetbrains.edu.learning.checkio.exceptions.LoginRequiredException;
import com.jetbrains.edu.learning.courseFormat.CheckStatus;
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask;
import com.jetbrains.edu.learning.ui.taskDescription.TaskDescriptionToolWindow;
import com.jetbrains.edu.python.learning.checkio.PyCheckiOCourseUpdater;
import com.jetbrains.edu.python.learning.checkio.messages.PyCheckiOErrorInformer;
import javafx.embed.swing.JFXPanel;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class PyCheckiOTaskChecker extends TaskChecker<EduTask> {
  private static final String TEST_RESULTS_ID = "checkioTestResults";

  public PyCheckiOTaskChecker(@NotNull EduTask task, @NotNull Project project) {
    super(task, project);
  }

  @Override
  public void onTaskSolved(@NotNull String message) {
    updateCourse();
    super.onTaskSolved(message);
  }

  private void updateCourse() {
    final CheckiOCourse course = (CheckiOCourse) StudyTaskManager.getInstance(project).getCourse();
    assert course != null;

    try {
      new PyCheckiOCourseUpdater(course, project).doUpdate();
    }
    catch (LoginRequiredException e) {
      LOG.warn(e);
      PyCheckiOErrorInformer.getInstance().showLoginRequiredMessage("Failed to update the course");
    }
    catch (NetworkException e) {
      LOG.warn(e);
      int result = PyCheckiOErrorInformer.getInstance().showNetworkErrorMessage("Failed to update the course");
      if (result == Messages.OK) {
        updateCourse();
      }
    }
    catch (ApiException e) {
      LOG.warn(e);
      PyCheckiOErrorInformer.getInstance().showErrorDialog("Something went wrong. Course cannot be updated.", "Failed to update the course");
    }
  }

  @NotNull
  @Override
  public CheckResult check() {
    final PyCheckiOMissionChecker missionChecker = new PyCheckiOMissionChecker(project, task);

    try {
      final CheckResult checkResult = ApplicationUtil.runWithCheckCanceled(missionChecker, ProgressManager.getInstance().getProgressIndicator());

      if (checkResult.getStatus() != CheckStatus.Unchecked) {
        showTestResultPanel(missionChecker.getBrowserPanel());
      }

      return checkResult;
    }
    catch (Exception e) {
      LOG.warn(e.getMessage());
      return CheckResult.FAILED_TO_CHECK;
    }
  }

  private void showTestResultPanel(@NotNull JFXPanel browserPanel) {
    final JPanel testResultPanel = new JPanel(new BorderLayout());

    testResultPanel.add(createBackButtonUI(), BorderLayout.PAGE_START);
    testResultPanel.add(createBrowserWindowUI(browserPanel));

    final TaskDescriptionToolWindow toolWindow = EduUtils.getStudyToolWindow(project);

    if (toolWindow != null) {
      toolWindow.getContentPanel().add(TEST_RESULTS_ID, testResultPanel);
      toolWindow.showPanelById(TEST_RESULTS_ID);
    }
  }

  @SuppressWarnings("MethodMayBeStatic")
  private JComponent createBrowserWindowUI(@NotNull JFXPanel browserPanel) {
    final JPanel browserWindowPanel = new JPanel();

    browserWindowPanel.setLayout(new BoxLayout(browserWindowPanel, BoxLayout.PAGE_AXIS));
    browserWindowPanel.add(browserPanel);

    return browserWindowPanel;
  }

  private JComponent createBackButtonUI() {
    final JLabel label = new JLabel("Back to task description", AllIcons.Vcs.Arrow_left, SwingConstants.LEFT);

    final JPanel buttonPanel = new JPanel(new BorderLayout());
    buttonPanel.add(Box.createRigidArea(new Dimension(0, 10)), BorderLayout.PAGE_START);
    buttonPanel.add(label, BorderLayout.WEST);
    buttonPanel.add(Box.createRigidArea(new Dimension(0, 10)), BorderLayout.PAGE_END);

    buttonPanel.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        showTaskInfoPanel();
      }
    });

    return buttonPanel;
  }

  private void showTaskInfoPanel() {
    final TaskDescriptionToolWindow descriptionToolWindow = EduUtils.getStudyToolWindow(project);
    if (descriptionToolWindow != null) {
      descriptionToolWindow.showPanelById(TaskDescriptionToolWindow.TASK_INFO_ID);
    }
  }
}
