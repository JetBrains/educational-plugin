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
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import javafx.embed.swing.JFXPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class JavaFxToolWindow extends TaskDescriptionToolWindow {
  private BrowserWindow myBrowserWindow;
  private JFXPanel taskSpecificPanel;

  public JavaFxToolWindow() {
    super();
  }

  @Override
  public JComponent createTaskInfoPanel(Project project) {
    myBrowserWindow = new BrowserWindow(project, true);
    JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
    panel.add(myBrowserWindow.getPanel());
    taskSpecificPanel = new JFXPanel();
    panel.add(taskSpecificPanel);
    return panel;
  }

  public void updateTaskSpecificPanel(@Nullable Task task) {
    taskSpecificPanel.setScene(JavaFxTaskSpecificPanel.createScene(task));
  }

  @Override
  public void updateFonts(@NotNull Project project) {
    super.setTaskText(project, myCurrentTask);
  }

  @Override
  public void setText(@NotNull String text) {
    myBrowserWindow.loadContent(text);
  }
}
