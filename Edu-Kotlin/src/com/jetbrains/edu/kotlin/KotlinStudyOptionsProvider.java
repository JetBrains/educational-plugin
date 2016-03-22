package com.jetbrains.edu.kotlin;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.ui.components.JBCheckBox;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.StudyTwitterPluginConfigurator;
import com.jetbrains.edu.learning.StudyUtils;
import com.jetbrains.edu.learning.settings.StudyOptionsProvider;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;


public class KotlinStudyOptionsProvider implements StudyOptionsProvider {
  private KotlinStudyTwitterSettings twitterSettings;
  private JBCheckBox myAskToTweetCheckBox;
  private JPanel myPanel;
  private boolean myIsModified = false;

  KotlinStudyOptionsProvider() {
    Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
    for (Project project : openProjects) {
      if (StudyTaskManager.getInstance(project).getCourse() != null) {
        StudyTwitterPluginConfigurator twitterConfigurator = StudyUtils.getTwitterConfigurator(project);
        if (twitterConfigurator != null) {
          twitterSettings = KotlinStudyTwitterSettings.getInstance(project);
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
