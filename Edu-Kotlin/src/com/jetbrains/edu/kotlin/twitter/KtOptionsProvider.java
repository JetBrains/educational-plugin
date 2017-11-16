package com.jetbrains.edu.kotlin.twitter;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.ui.components.JBCheckBox;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.TwitterPluginConfigurator;
import com.jetbrains.edu.learning.StudyUtils;
import com.jetbrains.edu.learning.settings.StudyOptionsProvider;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;


public class KtOptionsProvider implements StudyOptionsProvider {
  private KtTwitterSettings twitterSettings;
  private JBCheckBox myAskToTweetCheckBox;
  private JPanel myPanel;
  private boolean myIsModified = false;

  KtOptionsProvider() {
    Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
    for (Project project : openProjects) {
      if (StudyTaskManager.getInstance(project).getCourse() != null) {
        TwitterPluginConfigurator twitterConfigurator = StudyUtils.getTwitterConfigurator(project);
        if (twitterConfigurator != null) {
          twitterSettings = KtTwitterSettings.getInstance(project);
          myAskToTweetCheckBox.setSelected(twitterSettings.askToTweet());
          break;
        }
      }
    }
    myAskToTweetCheckBox.addActionListener(e -> myIsModified = true);
  }

  @Override
  public void apply() {
    twitterSettings.setAskToTweet(myAskToTweetCheckBox.isSelected());
  }

  @Override
  public void reset() {
    twitterSettings.setAskToTweet(true);
  }

  @Override
  public void disposeUIResources() {

  }

  @Nullable
  @Override
  public JComponent createComponent() {
    return myPanel;
  }

  @Override
  public boolean isModified() {
    return myIsModified;
  }
}
