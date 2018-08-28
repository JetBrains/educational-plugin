package com.jetbrains.edu.learning.checkio.checker;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ex.ApplicationUtil;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.checker.CheckResult;
import com.jetbrains.edu.learning.checker.TaskChecker;
import com.jetbrains.edu.learning.checkio.connectors.CheckiOOAuthConnector;
import com.jetbrains.edu.learning.courseFormat.CheckStatus;
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask;
import com.jetbrains.edu.learning.ui.taskDescription.TaskDescriptionToolWindow;
import javafx.embed.swing.JFXPanel;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class CheckiOTaskChecker extends TaskChecker<EduTask> {
  private static final String TEST_RESULTS_ID = "checkioTestResults";

  private final CheckiOMissionCheck myMissionCheck;

  protected CheckiOTaskChecker(
    @NotNull EduTask task,
    @NotNull Project project,
    @NotNull CheckiOOAuthConnector oAuthConnector,
    @NotNull String interpreterName,
    @NotNull String testFormTargetUrl
  ) {
    super(task, project);

    myMissionCheck = new CheckiOMissionCheck(
      task,
      project,
      oAuthConnector,
      interpreterName,
      testFormTargetUrl
    );
  }

  @NotNull
  @Override
  public CheckResult check() {
    try {
      final CheckResult checkResult =
        ApplicationUtil.runWithCheckCanceled(myMissionCheck, ProgressManager.getInstance().getProgressIndicator());

      if (checkResult.getStatus() != CheckStatus.Unchecked) {
        showTestResultPanel(myMissionCheck.getBrowserPanel());
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

  private static JComponent createBrowserWindowUI(@NotNull JFXPanel browserPanel) {
    final JPanel browserWindowPanel = new JPanel();

    browserWindowPanel.setLayout(new BoxLayout(browserWindowPanel, BoxLayout.PAGE_AXIS));
    browserWindowPanel.add(browserPanel);

    return browserWindowPanel;
  }

  private JComponent createBackButtonUI() {
    final JLabel label = new JBLabel("Back to task description", AllIcons.Vcs.Arrow_left, SwingConstants.LEFT);

    final JPanel buttonPanel = new JPanel(new BorderLayout());
    buttonPanel.add(Box.createRigidArea(JBUI.size(0, 10)), BorderLayout.PAGE_START);
    buttonPanel.add(label, BorderLayout.WEST);
    buttonPanel.add(Box.createRigidArea(JBUI.size(0, 10)), BorderLayout.PAGE_END);

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
