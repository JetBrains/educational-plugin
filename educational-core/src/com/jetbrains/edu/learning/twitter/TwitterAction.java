package com.jetbrains.edu.learning.twitter;

import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.project.Project;
import com.jetbrains.edu.learning.checker.CheckListener;
import com.jetbrains.edu.learning.TwitterPluginConfigurator;
import com.jetbrains.edu.learning.courseFormat.CheckStatus;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import org.jetbrains.annotations.NotNull;

public class TwitterAction implements CheckListener {

  private CheckStatus myStatusBeforeCheck;

  @Override
  public void beforeCheck(@NotNull Project project, @NotNull Task task) {
    myStatusBeforeCheck = task.getStatus();
  }

  @Override
  public void afterCheck(@NotNull Project project, @NotNull Task task) {
    for (TwitterPluginConfigurator twitterPluginConfigurator : Extensions.getExtensions(TwitterPluginConfigurator.EP_NAME)) {
      if (twitterPluginConfigurator.askToTweet(project, task, myStatusBeforeCheck)) {
        TwitterUtils.createTwitterDialogAndShow(project, twitterPluginConfigurator, task);
      }
    }
  }
}
