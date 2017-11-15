package com.jetbrains.edu.learning.actions;

import com.intellij.icons.AllIcons;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.navigation.StudyNavigator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NextTaskAction extends TaskNavigationAction {

  public static final String ACTION_ID = "Educational.NextTask";
  public static final String SHORTCUT = "ctrl pressed PERIOD";

  public NextTaskAction() {
    super("Next Task", "Navigate to the next task", AllIcons.Actions.Forward);
  }

  @Override
  protected Task getTargetTask(@NotNull final Task sourceTask) {
    return StudyNavigator.nextTask(sourceTask);
  }

  @NotNull
  @Override
  public String getActionId() {
    return ACTION_ID;
  }

  @Nullable
  @Override
  public String[] getShortcuts() {
    return new String[]{SHORTCUT};
  }
}