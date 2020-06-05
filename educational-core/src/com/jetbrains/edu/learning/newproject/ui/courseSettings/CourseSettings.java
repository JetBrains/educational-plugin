package com.jetbrains.edu.learning.newproject.ui.courseSettings;

import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class CourseSettings extends JPanel {

  private final JPanel myAdvancedSettings = new JPanel();
  private final HideableNoLineDecorator myDecorator;

  public CourseSettings() {
    super(new BorderLayout());
    myAdvancedSettings.setLayout(new BoxLayout(myAdvancedSettings, BoxLayout.Y_AXIS));
    add(myAdvancedSettings, BorderLayout.CENTER);

    myDecorator = new HideableNoLineDecorator(this, "&Settings");
    myDecorator.setContentComponent(myAdvancedSettings);
    myAdvancedSettings.setBorder(JBUI.Borders.empty(0, IdeBorderFactory.TITLED_BORDER_INDENT, 5, 0));
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

  public void setOn(boolean on) {
    myDecorator.setOn(on);
  }
}
