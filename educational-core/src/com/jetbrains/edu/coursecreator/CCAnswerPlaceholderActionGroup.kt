package com.jetbrains.edu.coursecreator;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.DumbAware;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("ComponentNotRegistered") // educational-core.xml
public class CCAnswerPlaceholderActionGroup extends DefaultActionGroup implements DumbAware {
  @Override
  public void update(@NotNull AnActionEvent e) {
    CCUtils.updateActionGroup(e);
  }
}
