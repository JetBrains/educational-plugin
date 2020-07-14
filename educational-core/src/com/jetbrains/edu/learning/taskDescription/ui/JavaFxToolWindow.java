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

import com.intellij.openapi.project.Project;
import com.intellij.util.ui.JBUI;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask;
import com.jetbrains.edu.learning.taskDescription.TaskDescriptionUtil;
import com.jetbrains.edu.learning.taskDescription.ui.styleManagers.ChoiceTaskResourcesManager;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.JFXPanel;
import netscape.javascript.JSObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;

import javax.swing.*;

public class JavaFxToolWindow extends TaskDescriptionToolWindow {
  private BrowserWindow myBrowserWindow;

  private BrowserWindow taskSpecificBrowserWindow;

  public JavaFxToolWindow(@NotNull Project project) {
    super(project);
  }

  @NotNull
  @Override
  public JComponent createTaskInfoPanel() {
    myBrowserWindow = new BrowserWindow(getProject(), true);
    return myBrowserWindow.getPanel();
  }

  @NotNull
  public JComponent createTaskSpecificPanel() {
    taskSpecificBrowserWindow = new BrowserWindow(getProject(), true);
    taskSpecificBrowserWindow.getPanel().setPreferredSize(JBUI.size(Integer.MAX_VALUE, 0));
    return taskSpecificBrowserWindow.getPanel();
  }

  public void updateTaskSpecificPanel(@Nullable Task task) {
    if (taskSpecificBrowserWindow == null) {
      return;
    }
    taskSpecificBrowserWindow.getPanel().setVisible(false);
    if (!(task instanceof ChoiceTask)) {
      return;
    }
    ChoiceTask choiceTask = (ChoiceTask)task;
    taskSpecificBrowserWindow.getPanel().setPreferredSize(JBUI.size(Integer.MAX_VALUE, 250));
    Platform.runLater(() -> addTextLoadedListener(choiceTask));
    taskSpecificBrowserWindow.loadContent(new ChoiceTaskResourcesManager().getText(choiceTask));
    taskSpecificBrowserWindow.getPanel().setVisible(true);
  }

  private void addTextLoadedListener(@NotNull ChoiceTask task) {
    taskSpecificBrowserWindow.getEngine().documentProperty().addListener(new ChangeListener<Document>() {
      @Override
      public void changed(ObservableValue<? extends Document> observable, Document oldValue, Document newValue) {
        if (newValue == null) {
          return;
        }
        JSObject window = (JSObject)taskSpecificBrowserWindow.getEngine().executeScript("window");
        window.setMember("task", task);
        if (taskSpecificBrowserWindow.getEngine().executeScript("document.getElementById('choiceOptions')") == null) {
          return;
        }
        int height =
          (Integer)taskSpecificBrowserWindow.getEngine().executeScript("document.getElementById('choiceOptions').scrollHeight");
        JFXPanel taskSpecificPanel = taskSpecificBrowserWindow.getPanel();
        taskSpecificPanel.setPreferredSize(JBUI.size(Integer.MAX_VALUE, height + 20));
        taskSpecificPanel.revalidate();
        taskSpecificBrowserWindow.getEngine().documentProperty().removeListener(this);
      }
    });
  }

  @Override
  protected void updateLaf() {
    myBrowserWindow.updateLaf();
  }

  @Override
  public void setText(@NotNull String text, @Nullable Task task) {
    if (task != null && TaskDescriptionUtil.containsYoutubeLink(text)) {
      text = TaskDescriptionUtil.processYoutubeLink(text, task.getId());
    }
    myBrowserWindow.loadContent(wrapHints(text, task));
  }

}
