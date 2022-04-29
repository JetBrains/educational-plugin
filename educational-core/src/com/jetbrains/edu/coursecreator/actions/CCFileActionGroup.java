package com.jetbrains.edu.coursecreator.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.DumbAware;
import com.jetbrains.edu.coursecreator.CCUtils;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("ComponentNotRegistered") // educational-core.xml
public class CCFileActionGroup extends DefaultActionGroup implements DumbAware{
  @Override
  public void update(@NotNull AnActionEvent e) {
    CCUtils.updateActionGroup(e);
  }
}
