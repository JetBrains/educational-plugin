package com.jetbrains.edu.python.learning.actions;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import org.jetbrains.annotations.NotNull;

public class PyWatchTutorialAction extends DumbAwareAction {
  public PyWatchTutorialAction() {
    super("Learn more about PyCharm Edu", null, null);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    BrowserUtil.browse("https://www.jetbrains.com/pycharm-edu/quickstart/");
  }
}
