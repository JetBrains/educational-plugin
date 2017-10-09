package com.jetbrains.edu.learning.newproject.ui;

import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.ui.HideableDecorator;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.List;

public class EduAdvancedSettings extends JPanel {

  private final JPanel myAdvancedSettings = new JPanel();

  public EduAdvancedSettings() {
    super(new BorderLayout());
    myAdvancedSettings.setLayout(new BoxLayout(myAdvancedSettings, BoxLayout.Y_AXIS));
    add(myAdvancedSettings, BorderLayout.CENTER);

    HideableDecorator decorator = new HideableDecorator(this, "Advanced Settings", false);
    decorator.setContentComponent(myAdvancedSettings);
    myAdvancedSettings.setBorder(JBUI.Borders.empty(0, IdeBorderFactory.TITLED_BORDER_INDENT, 5, 0));
  }

  public void setSettingComponents(LabeledComponent... settings) {
    setSettingsComponents(Arrays.asList(settings));
  }

  public void setSettingsComponents(List<LabeledComponent> settings) {
    myAdvancedSettings.removeAll();
    for (LabeledComponent setting : settings) {
      myAdvancedSettings.add(setting, BorderLayout.PAGE_END);
    }
    UIUtil.mergeComponentsWithAnchor(settings);
    myAdvancedSettings.revalidate();
    myAdvancedSettings.repaint();
  }
}
