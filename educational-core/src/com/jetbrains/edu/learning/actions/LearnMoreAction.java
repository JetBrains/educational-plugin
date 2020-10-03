package com.jetbrains.edu.learning.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.jetbrains.edu.learning.EduBrowser;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("ComponentNotRegistered") // educational-core.xml
public class LearnMoreAction extends DumbAwareAction {
  public LearnMoreAction() {
    super("Learn more about EduTools", null, null);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    EduBrowser.INSTANCE.browse("https://www.jetbrains.com/help/education/educational-products.html");
  }
}
