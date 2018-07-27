package com.jetbrains.edu.python.learning.checkio.checker;

import javax.swing.*;

public class CheckiOTestResultPanel extends JPanel {
  public static final String TEST_RESULTS_ID = "checkioTestResults";

  public CheckiOTestResultPanel(final CheckiOTestBrowserWindow browserWindow) {
    addBackButton();
    addBrowserWindow(browserWindow);
  }

  private void addBrowserWindow(CheckiOTestBrowserWindow window) {
    final JPanel browserPanel = new JPanel();
    browserPanel.setLayout(new BoxLayout(browserPanel, BoxLayout.PAGE_AXIS));
    browserPanel.add(window.getPanel());
    add(browserPanel);
  }

  private void addBackButton() {

  }
}
