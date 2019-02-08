package com.jetbrains.edu.learning.actions;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("ComponentNotRegistered") // educational-core.xml
public class LearnMoreAction extends DumbAwareAction {
  public LearnMoreAction() {
    super("Learn more about Edu Tools", null, null);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    BrowserUtil.browse("https://www.jetbrains.com/help/education/educational-products.html");
  }
}
