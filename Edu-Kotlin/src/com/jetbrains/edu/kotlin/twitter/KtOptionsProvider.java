package com.jetbrains.edu.kotlin.twitter;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBCheckBox;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.settings.OptionsProvider;
import com.jetbrains.edu.learning.twitter.TwitterPluginConfigurator;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;


public class KtOptionsProvider implements OptionsProvider {
  private final Project myProject;
  private KtTwitterSettings myTwitterSettings;
  private JBCheckBox myAskToTweetCheckBox;
  private JPanel myPanel;
  private boolean myIsModified = false;

  KtOptionsProvider(@NotNull final Project project) {
    myProject = project;
    myTwitterSettings = KtTwitterSettings.getInstance(myProject);
    TwitterPluginConfigurator twitterConfigurator = EduUtils.getTwitterConfigurator(myProject);
    if (twitterConfigurator != null) {
      myAskToTweetCheckBox.setSelected(myTwitterSettings.askToTweet());
    }
    myAskToTweetCheckBox.addActionListener(e -> myIsModified = true);
  }

  @Override
  public void apply() {
    myTwitterSettings.setAskToTweet(myAskToTweetCheckBox.isSelected());
  }

  @Override
  public void reset() {
    myTwitterSettings.setAskToTweet(true);
  }

  @Override
  public void disposeUIResources() {
  }

  @Nullable
  @Override
  public JComponent createComponent() {
    final boolean hasCourse = StudyTaskManager.getInstance(myProject).getCourse() != null;
    TwitterPluginConfigurator twitterConfigurator = EduUtils.getTwitterConfigurator(myProject);
    if (hasCourse && twitterConfigurator != null) {
      return myPanel;
    }
    return null;
  }

  @Override
  public boolean isModified() {
    return myIsModified;
  }

  @Nls
  @Override
  public String getDisplayName() {
    return "Twitter Settings";
  }
}
