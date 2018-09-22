/*
 * Copyright 2000-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jetbrains.edu.learning.ui.taskDescription;

import com.intellij.openapi.project.Project;
import com.intellij.ui.BrowserHyperlinkListener;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;

public class SwingToolWindow extends TaskDescriptionToolWindow {
  private JTextPane myTaskTextPane;
  private JPanel myTaskSpecificPanel;

  public SwingToolWindow() {
    super();
  }

  @Override
  public JComponent createTaskInfoPanel(Project project) {
    final JPanel panel = new JPanel(new BorderLayout());

    myTaskTextPane = SwingTaskUtil.createTextPaneWithStyleSheet();
    panel.add(new JBScrollPane(myTaskTextPane), BorderLayout.CENTER);
    myTaskTextPane.setBorder(JBUI.Borders.empty(20, 20, 0, 10));

    myTaskTextPane.addHyperlinkListener(e -> {
      String url = e.getDescription();
      if (url.startsWith(TaskDescriptionToolWindow.PSI_ELEMENT_PROTOCOL)) {
        if (e.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED)) {
          TaskDescriptionToolWindow.navigateToPsiElement(project, url);
        }
      } else {
        BrowserHyperlinkListener.INSTANCE.hyperlinkUpdate(e);
      }
    });

    return panel;
  }

  public JComponent createTaskSpecificPanel(Task currentTask) {
    myTaskSpecificPanel = new JPanel(new BorderLayout());
    return myTaskSpecificPanel;
  }

  @Override
  public void updateTaskSpecificPanel(@Nullable Task task) {
    myTaskSpecificPanel.removeAll();
    final JPanel panel = SwingTaskUtil.createSpecificPanel(task);
    if (panel != null) {
      myTaskSpecificPanel.add(panel, BorderLayout.CENTER);
      myTaskSpecificPanel.revalidate();
      myTaskSpecificPanel.repaint();
    }
  }

  public void setText(@NotNull String text) {
    myTaskTextPane.setText(text);
  }
}

