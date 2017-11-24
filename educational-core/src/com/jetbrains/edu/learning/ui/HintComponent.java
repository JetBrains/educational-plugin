package com.jetbrains.edu.learning.ui;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.ui.BrowserHyperlinkListener;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;

public class HintComponent extends SimpleToolWindowPanel implements DataProvider, Disposable {
  private JTextPane myTaskTextPane;
  private JPanel myContentPanel;

  public HintComponent() {
    super(true, true);
    myContentPanel = new JPanel(new BorderLayout());
    JComponent hintPanel = createHintPanel();
    myContentPanel.add(hintPanel, BorderLayout.CENTER);
    setContent(myContentPanel);
  }

  public JComponent createHintPanel() {
    myTaskTextPane = new JTextPane();
    final JBScrollPane scrollPane = new JBScrollPane(myTaskTextPane);
    final HTMLEditorKit htmlEditorKit = UIUtil.getHTMLEditorKit(false);
    myTaskTextPane.setContentType(htmlEditorKit.getContentType());
    myTaskTextPane.setEditorKit(htmlEditorKit);

    myTaskTextPane.setEditable(false);
    if (!UIUtil.isUnderDarcula()) {
      myTaskTextPane.setBackground(EditorColorsManager.getInstance().getGlobalScheme().getDefaultBackground());
    }
    myTaskTextPane.setBorder(JBUI.Borders.empty(20, 20, 0, 10));
    myTaskTextPane.addHyperlinkListener(BrowserHyperlinkListener.INSTANCE);
    return scrollPane;
  }

  public void setActionToolbar(DefaultActionGroup group) {
    JPanel toolbarPanel = createToolbarPanel(group);
    setToolbar(toolbarPanel);
  }

  public static JPanel createToolbarPanel(ActionGroup group) {
    final ActionToolbar actionToolBar =
        ActionManager.getInstance().createActionToolbar("Study", group, true);
    return JBUI.Panels.simplePanel(actionToolBar.getComponent());
  }

  public void setText(@NotNull String text) {
    myTaskTextPane.setText(text);
  }

  public String getText() {
    final Document document = myTaskTextPane.getDocument();
    try {
      return document.getText(1, document.getLength());
    } catch (BadLocationException e) {
      return myTaskTextPane.getText();
    }
  }

  @Override
  public void dispose() {
    myContentPanel.removeAll();
    myContentPanel = null;
  }
}
