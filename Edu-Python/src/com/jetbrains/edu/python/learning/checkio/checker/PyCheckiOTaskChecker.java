package com.jetbrains.edu.python.learning.checkio.checker;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ex.ApplicationUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.jetbrains.edu.learning.checker.CheckResult;
import com.jetbrains.edu.learning.checker.TaskChecker;
import com.jetbrains.edu.learning.checkio.CheckiOCourseContentGenerator;
import com.jetbrains.edu.learning.checkio.CheckiOCourseUpdater;
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOCourse;
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOMission;
import com.jetbrains.edu.learning.checkio.notifications.errors.handlers.CheckiOErrorHandler;
import com.jetbrains.edu.learning.courseFormat.CheckStatus;
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask;
import com.jetbrains.edu.python.learning.checkio.connectors.PyCheckiOApiConnector;
import com.jetbrains.edu.python.learning.checkio.connectors.PyCheckiOOAuthConnector;
import com.jetbrains.python.PythonFileType;
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
    final CheckiOCourse course = (CheckiOCourse) task.getCourse();

    final CheckiOCourseContentGenerator contentGenerator =
      new CheckiOCourseContentGenerator(PythonFileType.INSTANCE, PyCheckiOApiConnector.getInstance());

    try {
      new CheckiOCourseUpdater(
        course,
        project,
        contentGenerator
      ).doUpdate();
    }
    catch (Exception e) {
      new CheckiOErrorHandler(
        "Failed to update the course",
        PyCheckiOOAuthConnector.getInstance()
      ).handle(e);
    }
  }

  @NotNull
  @Override
  public CheckResult check(@NotNull ProgressIndicator indicator) {
    assert task instanceof CheckiOMission;
    final PyCheckiOMissionCheck missionCheck = new PyCheckiOMissionCheck(project, (CheckiOMission)task);

    try {
      final CheckResult checkResult =
        ApplicationUtil.runWithCheckCanceled(missionCheck, indicator);

      if (checkResult.getStatus() != CheckStatus.Unchecked) {
        showTestResultPanel(missionCheck.getBrowserPanel());
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

    //TODO: integrate checkio into new task description panel
    //final TaskDescriptionToolWindow toolWindow = EduUtils.getStudyToolWindow(project);
    //
    //if (toolWindow != null) {
    //
    //  toolWindow.getContentPanel().add(TEST_RESULTS_ID, testResultPanel);
    //  toolWindow.showPanelById();
    //}
  }

  private static JComponent createBrowserWindowUI(@NotNull JFXPanel browserPanel) {
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
    //TODO: integrate checkio into new task description panel
    //final TaskDescriptionToolWindow descriptionToolWindow = EduUtils.getStudyToolWindow(project);
    //if (descriptionToolWindow != null) {
    //  descriptionToolWindow.showPanelById();
    //}
  }
}
