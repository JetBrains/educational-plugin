/*
 * Copyright 2000-2021 JetBrains s.r.o.
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

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.nodes.Element;

import javax.swing.*;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.net.URL;

public class SwingToolWindow extends TaskDescriptionToolWindow {
  private static final Logger LOG = Logger.getInstance(SwingToolWindow.class);

  // all a tagged elements should have different href otherwise they are all underlined on hover. That's why
  // we have to add hint number to href
  private static final String HINT_BLOCK_TEMPLATE = "  <img src='%s' width='16' height='16' >" +
                                                    "  <span><a href='hint://%s', value='%s'>Hint %s</a>" +
                                                    "  <img src='%s' width='16' height='16' >";
  private static final String HINT_EXPANDED_BLOCK_TEMPLATE = "  <img src='%s' width='16' height='16' >" +
                                                             "  <span><a href='hint://%s', value='%s'>Hint %s</a>" +
                                                             "  <img src='%s' width='16' height='16' >" +
                                                             "  <div class='hint_text'>%s</div>";
  private JTextPane myTaskTextPane;
  private JPanel myTaskSpecificPanel;

  public SwingToolWindow(@NotNull Project project) {
    super(project);
  }

  @NotNull
  @Override
  public JComponent createTaskInfoPanel() {
    final JPanel panel = new JPanel(new BorderLayout());

    // we are using HTMLEditorKit here because otherwise styles are not applied
    HTMLEditorKit editorKit = new HTMLEditorKit();
    editorKit.setStyleSheet(null);

    myTaskTextPane = SwingTaskUtil.createTextPane(editorKit);
    JBScrollPane scrollPane = new JBScrollPane(myTaskTextPane);
    scrollPane.setBorder(null);
    panel.add(scrollPane, BorderLayout.CENTER);
    myTaskTextPane.setBorder(JBUI.Borders.empty(20, 0, 0, 10));

    SwingToolWindowLinkHandler toolWindowLinkHandler = new SwingToolWindowLinkHandler(getProject(), myTaskTextPane);
    myTaskTextPane.addHyperlinkListener(toolWindowLinkHandler.getHyperlinkListener());

    return panel;
  }

  @NotNull
  public JComponent createTaskSpecificPanel() {
    myTaskSpecificPanel = new JPanel(new BorderLayout());
    return myTaskSpecificPanel;
  }

  @Override
  public void updateTaskSpecificPanel(@Nullable Task task) {
    if (myTaskSpecificPanel == null) return;
    myTaskSpecificPanel.removeAll();
    final JPanel panel = SwingTaskUtil.createSpecificPanel(task);
    if (panel != null) {
      myTaskSpecificPanel.add(panel, BorderLayout.CENTER);
      myTaskSpecificPanel.revalidate();
      myTaskSpecificPanel.repaint();
    }
  }

  public void setText(@NotNull String text, @Nullable Task task) {
    myTaskTextPane.setText(TaskUtils.htmlWithResources(getProject(), wrapHints(text, task)));
  }

  @NotNull
  @Override
  protected String wrapHint(@NotNull Element hintElement, @NotNull String displayedHintNumber) {
    return wrapHint(getProject(), hintElement, displayedHintNumber);
  }

  @NotNull
  public static String wrapHint(@NotNull Project project,
                                @NotNull Element hintElement,
                                @NotNull String displayedHintNumber) {
    String bulbIcon = getIconFullPath("style/hint/swing/swing_icons/retina_bulb.png", "/style/hint/swing/swing_icons/bulb.png");
    String hintText = hintElement.html();
    if (displayedHintNumber.isEmpty() || displayedHintNumber.equals("1")) {
      hintElement.wrap("<div class='top'></div>");
    }

    Course course = StudyTaskManager.getInstance(project).getCourse();
    if (course != null && !course.isStudy()) {
      String downIcon = getIconFullPath("/style/hint/swing/swing_icons/retina_down.png", "/style/hint/swing/swing_icons/down.png");
      return String.format(HINT_EXPANDED_BLOCK_TEMPLATE, bulbIcon, displayedHintNumber, hintText, displayedHintNumber, downIcon, hintText);
    }
    else {
      String leftIcon = getIconFullPath("/style/hint/swing/swing_icons/retina_right.png", "/style/hint/swing/swing_icons/right.png");
      return String.format(HINT_BLOCK_TEMPLATE, bulbIcon, displayedHintNumber, hintText, displayedHintNumber, leftIcon);
    }
  }

  private static String getIconFullPath(String retinaPath, String path) {
    String bulbPath = UIUtil.isRetina() ? retinaPath : path;
    URL bulbIconUrl = SwingToolWindow.class.getClassLoader().getResource(bulbPath);
    if (bulbIconUrl == null) {
      LOG.warn("Cannot find bulb icon");
    }
    return bulbIconUrl == null ? "" : bulbIconUrl.toExternalForm();
  }
}

