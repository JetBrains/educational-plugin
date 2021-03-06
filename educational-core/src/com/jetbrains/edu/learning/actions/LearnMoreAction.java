package com.jetbrains.edu.learning.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.jetbrains.edu.learning.EduBrowser;
import com.jetbrains.edu.learning.messages.EduCoreBundle;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("ComponentNotRegistered") // educational-core.xml
public class LearnMoreAction extends DumbAwareAction {
  public LearnMoreAction() {
    super(EduCoreBundle.lazyMessage("action.learn.more.text"));
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    EduBrowser.getInstance().browse("https://www.jetbrains.com/help/education/educational-products.html");
  }
}
