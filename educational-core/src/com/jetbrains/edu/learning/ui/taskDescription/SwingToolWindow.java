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

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.BrowserHyperlinkListener;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import java.awt.*;
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

public class SwingToolWindow extends TaskDescriptionToolWindow {
  private static final Logger LOG = Logger.getInstance(SwingToolWindow.class);
  private static final String HINT_PROTOCOL = "hint://";

  // all a tagged elements should have different href otherwise they are all underlined on hover. That's why
  // we have to add hint number to href
  private static final String HINT_BLOCK_TEMPLATE = "  <img src='%s' width='16' height='16' >" +
                                                    "  <span><a href='hint://%s', value='%s'>Hint %s</a>" +
                                                    "  <img src='%s' width='16' height='16' >";
  private static final String HINT_EXPANDED_BLOCK_TEMPLATE = "  <img src='%s' width='16' height='16' >" +
                                                             "  <span><a href='hint://%s', value='%s'>Hint %s</a>" +
                                                             "  <img src='%s' width='16' height='16' >" +
                                                             "  <div class='hint_text'>%s</div>";
  private static final String HINT_TEXT_PATTERN = "<div class='hint_text'>%s</div>";
  private JTextPane myTaskTextPane;
  private JPanel myTaskSpecificPanel;
  private Project myProject;

  public SwingToolWindow() {
    super();
  }

  @Override
  public JComponent createTaskInfoPanel(@NotNull Project project) {
    myProject = project;
    final JPanel panel = new JPanel(new BorderLayout());

    myTaskTextPane = SwingTaskUtil.createTextPane();
    JBScrollPane scrollPane = new JBScrollPane(myTaskTextPane);
    scrollPane.setBorder(null);
    panel.add(scrollPane, BorderLayout.CENTER);
    myTaskTextPane.setBorder(JBUI.Borders.empty(20, 0, 0, 10));
    myTaskTextPane.addHyperlinkListener(new EduHyperlinkListener(project));

    return panel;
  }

  public JComponent createTaskSpecificPanel(Task currentTask) {
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

  public void setText(@NotNull String text) {
    String wrappedText = wrapHints(text);
    myTaskTextPane.setText(JavaFxTaskUtil.htmlWithResources(myProject, wrappedText));
  }

  @Override
  protected String wrapHint(@NotNull org.jsoup.nodes.Element hintElement, @NotNull String displayedHintNumber) {
    String bulbIcon = getIconFullPath("style/hint/swing/swing_icons/retina_bulb.png", "/style/hint/swing/swing_icons/bulb.png");
    String hintText = hintElement.html();
    if (displayedHintNumber.isEmpty() || displayedHintNumber.equals("1")) {
      hintElement.wrap("<div class='top'></div>");
    }

    Course course = StudyTaskManager.getInstance(myProject).getCourse();
    if (course != null && !course.isStudy()) {
      String downIcon = getIconFullPath("/style/hint/swing/swing_icons/retina_down.png", "/style/hint/swing/swing_icons/down.png");
      return String.format(HINT_EXPANDED_BLOCK_TEMPLATE, bulbIcon, displayedHintNumber, hintText, displayedHintNumber, downIcon, hintText);
    }
    else {
      String leftIcon = getIconFullPath("/style/hint/swing/swing_icons/retina_right.png", "/style/hint/swing/swing_icons/right.png");
      return String.format(HINT_BLOCK_TEMPLATE, bulbIcon, displayedHintNumber, hintText, displayedHintNumber, leftIcon);
    }
  }

  private String getIconFullPath(String retinaPath, String path) {
    String bulbPath = UIUtil.isRetina() ? retinaPath : path;
    URL bulbIconUrl = getClass().getClassLoader().getResource(bulbPath);
    if (bulbIconUrl == null) {
      LOG.warn("Cannot find bulb icon");
    }
    return bulbIconUrl == null ? "" : bulbIconUrl.toExternalForm();
  }

  @Override
  protected void updateLaf() {
    myTaskTextPane.setBackground(EditorColorsManager.getInstance().getGlobalScheme().getDefaultBackground());
  }

  class EduHyperlinkListener implements HyperlinkListener {
    private Project myProject;

    public EduHyperlinkListener(@NotNull Project project) {
      myProject = project;
    }

    @Override
    public void hyperlinkUpdate(HyperlinkEvent event) {
      if (event.getEventType() != HyperlinkEvent.EventType.ACTIVATED) {
        return;
      }

      String url = event.getDescription();
      if (url.startsWith(TaskDescriptionToolWindow.PSI_ELEMENT_PROTOCOL)) {
        TaskDescriptionToolWindow.navigateToPsiElement(myProject, url);
        return;
      }

      if (url.startsWith(HINT_PROTOCOL)) {
        Element sourceElement = event.getSourceElement();
        toggleHintElement(sourceElement);
        return;
      }

      BrowserHyperlinkListener.INSTANCE.hyperlinkUpdate(event);
    }

    private void toggleHintElement(@NotNull Element sourceElement) {
      try {
        HTMLDocument document = (HTMLDocument)myTaskTextPane.getDocument();
        Element parent = sourceElement.getParentElement();
        String className = (String)parent.getParentElement().getAttributes().getAttribute(HTML.Attribute.CLASS);
        if (!"hint".equals(className)) {
          LOG.warn(String.format("Div element with hint class not found. Course: %s", StudyTaskManager.getInstance(myProject).getCourse()));
          return;
        }

        Element hintTextElement = getHintTextElement(parent);
        if (hintTextElement == null) {
          String downPath = UIUtil.isRetina() ? "style/hint/swing/swing_icons/retina_down.png" : "style/hint/swing/swing_icons/down.png";
          changeArrowIcon(sourceElement, document, downPath);

          Object hintText = ((SimpleAttributeSet)sourceElement.getAttributes().getAttribute(HTML.Tag.A)).getAttribute(HTML.Attribute.VALUE);
          document.insertBeforeEnd(parent.getParentElement(), String.format(HINT_TEXT_PATTERN, hintText));
          EduCounterUsageCollector.hintExpanded();
        }
        else {
          String leftPath = UIUtil.isRetina() ? "style/hint/swing/swing_icons/retina_right.png" : "style/hint/swing/swing_icons/right.png";
          changeArrowIcon(sourceElement, document, leftPath);
          document.removeElement(hintTextElement);
          EduCounterUsageCollector.hintCollapsed();
        }
      }
      catch (BadLocationException | IOException e) {
        LOG.warn(e.getMessage());
      }
    }

    private void changeArrowIcon(@NotNull Element sourceElement, @NotNull HTMLDocument document, @NotNull String iconUrl)
      throws BadLocationException, IOException {
      URL resource = getClass().getClassLoader().getResource(iconUrl);
      if (resource != null) {
        Element arrowImageElement = getArrowImageElement(sourceElement.getParentElement());
        document.setOuterHTML(arrowImageElement, String.format("<img src='%s' width='16' height='16'>", resource.toExternalForm()));
      }
      else {
        LOG.warn("Cannot find arrow icon " + iconUrl);
      }
    }

    @Nullable
    private Element getArrowImageElement(@NotNull Element element) {
      Element result = null;
      for (int i = 0; i < element.getElementCount(); i++) {
        Element child = element.getElement(i);
        if (child == null) {
          continue;
        }

        AttributeSet attributes = child.getAttributes();
        if (attributes == null) {
          continue;
        }

        Object img = attributes.getAttribute(HTML.Attribute.SRC);
        if (img instanceof String
            && (((String)img).endsWith("down.png") || ((String)img).endsWith("right.png"))) {
          result = child;
        }
      }
      return result;
    }

    @Nullable
    private Element getHintTextElement(@NotNull Element parent) {
      Element hintTextElement = null;
      Enumeration children = ((HTMLDocument.AbstractElement)parent).getParent().children();
      while (children.hasMoreElements()) {
        Element child = (Element)children.nextElement();
        AttributeSet childAttributes = child.getAttributes();
        String childClassName = (String)childAttributes.getAttribute(HTML.Attribute.CLASS);
        if ("hint_text".equals(childClassName)) {
          hintTextElement = child;
          break;
        }
      }

      return hintTextElement;
    }
  }
}

