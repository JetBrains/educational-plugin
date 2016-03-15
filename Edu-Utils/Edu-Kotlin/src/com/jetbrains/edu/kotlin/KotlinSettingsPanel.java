package com.jetbrains.edu.kotlin;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.util.ui.UIUtil;
import com.jetbrains.edu.learning.settings.ModifiableSettingsPanel;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;


public class KotlinSettingsPanel implements ModifiableSettingsPanel {
  private JBCheckBox myAskToTweetCheckBox;
  private JPanel myPanel;
  private boolean myIsModified = false;
  private Project myProject;

  public KotlinSettingsPanel(Project project) {
    myProject = project;
    myAskToTweetCheckBox.addActionListener(e -> myIsModified = true);
    myAskToTweetCheckBox.setSelected(KotlinStudyTwitterSettings.getInstance(myProject).askToTweet());
    myPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIUtil.getBoundsColor()));
  }

  @Override
  public void apply() {
    KotlinStudyTwitterSettings.getInstance(myProject).setAskToTweet(myAskToTweetCheckBox.isSelected());
  }

  @Override
  public void reset() {
    KotlinStudyTwitterSettings.getInstance(myProject).setAskToTweet(true);
  }

  @Override
  public void resetCredentialsModification() {
    myIsModified = false;
  }

  @Override
  public boolean isModified() {
    return myIsModified;
  }

  @NotNull
  public JPanel getPanel() {
    return myPanel;
  }
}
