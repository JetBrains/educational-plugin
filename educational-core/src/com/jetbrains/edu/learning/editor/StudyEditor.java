package com.jetbrains.edu.learning.editor;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.impl.EditorImpl;
import com.intellij.openapi.fileEditor.impl.text.PsiAwareTextEditorImpl;
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBLoadingPanel;
import com.intellij.ui.components.labels.ActionLink;
import com.intellij.util.ui.JBUI;
import com.jetbrains.edu.learning.StudyUtils;
import com.jetbrains.edu.learning.actions.RefreshTaskFileAction;
import com.jetbrains.edu.learning.core.EduDocumentListener;
import com.jetbrains.edu.learning.courseFormat.TaskFile;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of StudyEditor which has panel with special buttons and task text
 * also @see {@link StudyFileEditorProvider}
 */
public class StudyEditor extends PsiAwareTextEditorImpl {
  public static final String BROKEN_SOLUTION_ERROR_TEXT_START = "Solution can't be loaded.";
  public static final String BROKEN_SOLUTION_ERROR_TEXT_END = " to solve it again";
  public static final String ACTION_TEXT = "Reset task";
  private final TaskFile myTaskFile;
  private static final Map<Document, EduDocumentListener> myDocumentListeners = new HashMap<>();

  public StudyEditor(@NotNull final Project project, @NotNull final VirtualFile file) {
    super(project, file, TextEditorProvider.getInstance());
    myTaskFile = StudyUtils.getTaskFile(project, file);

    validateTaskFile();
  }

  public void validateTaskFile() {
    if (!myTaskFile.isValid(getEditor().getDocument().getText())) {
      JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
      panel.add(new JLabel(BROKEN_SOLUTION_ERROR_TEXT_START));
      ActionLink actionLink = new ActionLink(ACTION_TEXT, new RefreshTaskFileAction());
      actionLink.setVerticalAlignment(SwingConstants.CENTER);
      panel.add(actionLink);
      panel.add(new JLabel(BROKEN_SOLUTION_ERROR_TEXT_END));
      panel.setBorder(BorderFactory.createEmptyBorder(JBUI.scale(5), JBUI.scale(5), JBUI.scale(5), 0));
      getEditor().setHeaderComponent(panel);
    }
    else {
      getEditor().setHeaderComponent(null);
    }
  }

  public TaskFile getTaskFile() {
    return myTaskFile;
  }

  public static void addDocumentListener(@NotNull final Document document, @NotNull final EduDocumentListener listener) {
    document.addDocumentListener(listener);
    myDocumentListeners.put(document, listener);
  }

  public static void removeListener(Document document) {
    final EduDocumentListener listener = myDocumentListeners.get(document);
    if (listener != null) {
      document.removeDocumentListener(listener);
    }
    myDocumentListeners.remove(document);
  }

  public void showLoadingPanel() {
    JBLoadingPanel component = getComponent();
    ((EditorImpl)getEditor()).setViewer(true);
    component.setLoadingText("Loading solution");
    component.startLoading();
  }
}
