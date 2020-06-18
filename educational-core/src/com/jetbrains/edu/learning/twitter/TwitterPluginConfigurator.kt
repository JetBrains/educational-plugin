package com.jetbrains.edu.learning.twitter;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.jetbrains.edu.learning.courseFormat.CheckStatus;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Provides twitting for courses
 *
 * @see TwitterAction
 */
public interface TwitterPluginConfigurator {
  ExtensionPointName<TwitterPluginConfigurator> EP_NAME = ExtensionPointName.create("Educational.twitterPluginConfigurator");

  /**
   * The implementation should define policy when user will be asked to tweet.
   */
  boolean askToTweet(@NotNull Project project, @NotNull Task solvedTask, @NotNull CheckStatus statusBeforeCheck);

  /**
   * @return panel that will be shown to user in ask to tweet dialog. 
   */
  @Nullable
  TwitterUtils.TwitterDialogPanel getTweetDialogPanel(@NotNull Task solvedTask);
}
