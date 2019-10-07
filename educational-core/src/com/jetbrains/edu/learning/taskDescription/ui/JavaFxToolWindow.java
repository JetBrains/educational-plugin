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
package com.jetbrains.edu.learning.taskDescription.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.nodes.Element;

import javax.swing.*;

public class JavaFxToolWindow extends TaskDescriptionToolWindow {
  private BrowserWindow myBrowserWindow;
  public static final String HINT_HEADER = "hint_header";
  public static final String HINT_HEADER_EXPANDED = HINT_HEADER + " checked";
  private static final String HINT_BLOCK_TEMPLATE = "<div class='" + HINT_HEADER + "'>Hint %s</div>" +
                                                    "  <div class='hint_content'>" +
                                                    " %s" +
                                                    "  </div>";
  private static final String HINT_EXPANDED_BLOCK_TEMPLATE = "<div class='" + HINT_HEADER_EXPANDED + "'>Hint %s</div>" +
                                                             "  <div class='hint_content'>" +
                                                             " %s" +
                                                             "  </div>";

  private Project myProject;
  private JFXPanel taskSpecificPanel;

  public JavaFxToolWindow() {
    super();
  }

  @NotNull
  @Override
  public JComponent createTaskInfoPanel(@NotNull Project project) {
    myProject = project;
    myBrowserWindow = new BrowserWindow(project, true);
    return myBrowserWindow.getPanel();
  }

  @NotNull
  public JComponent createTaskSpecificPanel(@Nullable Task task) {
    taskSpecificPanel = new JFXPanel();
    return taskSpecificPanel;
  }

  public void updateTaskSpecificPanel(@Nullable Task task) {
    if (taskSpecificPanel == null) return;
    final Scene scene = task != null ? JavaFxTaskUtil.createScene(task) : null;
    Platform.runLater(() -> {
      taskSpecificPanel.setScene(scene);
    });

    ApplicationManager.getApplication().invokeLater(() -> {
      taskSpecificPanel.setVisible(scene != null);
    });
  }

  @NotNull
  @Override
  protected String wrapHint(@NotNull Element hintElement, @NotNull String displayedHintNumber) {
    Course course = StudyTaskManager.getInstance(myProject).getCourse();
    String hintText = hintElement.html();
    if (course == null) {
      return String.format(HINT_BLOCK_TEMPLATE, displayedHintNumber, hintText);
    }

    boolean study = course.isStudy();
    if (study) {
      return String.format(HINT_BLOCK_TEMPLATE, displayedHintNumber, hintText);
    }
    else {
      return String.format(HINT_EXPANDED_BLOCK_TEMPLATE, displayedHintNumber, hintText);
    }
  }

  @Override
  protected void updateLaf() {
    myBrowserWindow.updateLaf();
  }

  @Override
  public void setText(@NotNull String text, @Nullable Task task) {
    myBrowserWindow.loadContent(wrapHints(text, task), task);
  }

}
