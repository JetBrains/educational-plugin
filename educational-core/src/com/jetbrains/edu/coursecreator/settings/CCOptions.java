package com.jetbrains.edu.coursecreator.settings;

import com.intellij.openapi.application.Experiments;
import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.ui.components.JBCheckBox;
import com.jetbrains.edu.coursecreator.actions.CCPluginToggleAction;
import com.jetbrains.edu.learning.EduExperimentalFeatures;
import com.jetbrains.edu.learning.settings.OptionsProvider;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class CCOptions implements OptionsProvider {
  private JRadioButton myHtmlRadioButton;
  private JRadioButton myMarkdownRadioButton;
  private JPanel myPanel;
  private JPanel myCustomOptions;

  private JBCheckBox myShowSplitEditorCheckBox = new JBCheckBox(null, CCSettings.getInstance().showSplitEditor());

  @Nullable
  @Override
  public JComponent createComponent() {
    if (!CCPluginToggleAction.isCourseCreatorFeaturesEnabled()) return null;
    if (CCSettings.getInstance().useHtmlAsDefaultTaskFormat()) {
      myHtmlRadioButton.setSelected(true);
      IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(
        () -> IdeFocusManager.getGlobalInstance().requestFocus(myHtmlRadioButton, true));
    }
    else {
      myMarkdownRadioButton.setSelected(true);
      IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(
        () -> IdeFocusManager.getGlobalInstance().requestFocus(myMarkdownRadioButton, true));
    }
    if (Experiments.isFeatureEnabled(EduExperimentalFeatures.SPLIT_EDITOR)) {
      LabeledComponent<JBCheckBox> showSplitEditorComponent =
        LabeledComponent.create(myShowSplitEditorCheckBox, "Show previous task file in framework lessons", BorderLayout.WEST);
      myCustomOptions.add(showSplitEditorComponent);
    }
    return myPanel;
  }

  @Override
  public boolean isModified() {
    CCSettings settings = CCSettings.getInstance();
    return myHtmlRadioButton.isSelected() != settings.useHtmlAsDefaultTaskFormat() ||
           myShowSplitEditorCheckBox.isSelected() != settings.showSplitEditor();
  }

  @Override
  public void apply() {
    if (isModified()) {
      CCSettings.getInstance().setUseHtmlAsDefaultTaskFormat(myHtmlRadioButton.isSelected());
      CCSettings.getInstance().setShowSplitEditor(myShowSplitEditorCheckBox.isSelected());
    }
  }

  @Override
  public void reset() {
    CCSettings settings = CCSettings.getInstance();
    myHtmlRadioButton.setSelected(settings.useHtmlAsDefaultTaskFormat());
    myMarkdownRadioButton.setSelected(!settings.useHtmlAsDefaultTaskFormat());
    myShowSplitEditorCheckBox.setSelected(settings.showSplitEditor());
  }

  @Override
  public void disposeUIResources() {

  }

  @Nls
  @Override
  public String getDisplayName() {
    return "Course Creator options";
  }
}
