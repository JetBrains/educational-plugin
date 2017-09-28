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
package com.jetbrains.edu.learning.ui;

import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.project.Project;
import com.intellij.ui.BrowserHyperlinkListener;
import com.intellij.ui.ColorUtil;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import java.awt.*;

public class StudySwingToolWindow extends StudyToolWindow {
  private JTextPane myTaskTextPane;

  public StudySwingToolWindow() {
    super();
  }

  @Override
  public JComponent createTaskInfoPanel(Project project) {
    myTaskTextPane = new JTextPane();
    final JBScrollPane scrollPane = new JBScrollPane(myTaskTextPane);
    myTaskTextPane.setContentType(new HTMLEditorKit().getContentType());

    final EditorColorsScheme editorColorsScheme = EditorColorsManager.getInstance().getGlobalScheme();
    int fontSize = editorColorsScheme.getEditorFontSize();
    final String fontName = editorColorsScheme.getEditorFontName();
    final Font font = new Font(fontName, Font.PLAIN, fontSize);
    String dimmedColor = ColorUtil.toHex(ColorUtil.dimmer(UIUtil.getPanelBackground()));
    int size = font.getSize();
    String bodyRule = String.format("body { font-family: %s; font-size: %dpt; }", font.getFamily(), size);
    String preRule = String.format("pre {font-family: Courier; font-size: %dpt; " +
      "display: inline; ine-height: 50px; padding-top: 5px; padding-bottom: 5px; " +
      "padding-left: 5px; background-color:%s;}", fontSize, dimmedColor);
    String codeRule = String.format("code {font-family: Courier; font-size:%dpt; display: flex; " +
      "float: left; background-color: %s;}", fontSize, dimmedColor);
    String sizeRule = String.format("h1 { font-size: %dpt; } h2 { font-size: %fpt; }", 2 * fontSize, 1.5 * fontSize);
    HTMLEditorKit htmlEditorKit = UIUtil.getHTMLEditorKit(false);

    StyleSheet styleSheet = htmlEditorKit.getStyleSheet();
    styleSheet.addRule(bodyRule);
    styleSheet.addRule(preRule);
    styleSheet.addRule(codeRule);
    styleSheet.addRule(sizeRule);

    myTaskTextPane.setEditorKit(htmlEditorKit);

    myTaskTextPane.setEditable(false);
    if (!UIUtil.isUnderDarcula()) {
      myTaskTextPane.setBackground(EditorColorsManager.getInstance().getGlobalScheme().getDefaultBackground());
    }
    myTaskTextPane.setBorder(JBUI.Borders.empty(20, 20, 0, 10));
    myTaskTextPane.addHyperlinkListener(BrowserHyperlinkListener.INSTANCE);
    return scrollPane;
  }

  public void setText(@NotNull String text) {
    myTaskTextPane.setText(text);
  }
}

