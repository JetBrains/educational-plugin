package com.jetbrains.edu.learning.editor;

import com.intellij.openapi.editor.impl.EditorImpl;
import com.intellij.openapi.fileEditor.impl.text.PsiAwareTextEditorImpl;
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBLoadingPanel;
import com.intellij.ui.components.labels.ActionLink;
import com.intellij.util.ui.JBUI;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.actions.RefreshTaskFileAction;
import com.jetbrains.edu.learning.courseFormat.TaskFile;
import com.jetbrains.edu.learning.placeholderDependencies.PlaceholderDependencyManager;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

public class EduEditor extends PsiAwareTextEditorImpl {
  public static final String BROKEN_SOLUTION_ERROR_TEXT_START = "Solution can't be loaded.";
  public static final String BROKEN_SOLUTION_ERROR_TEXT_END = " to solve it again";
  public static final String ACTION_TEXT = "Reset task";
  private final TaskFile myTaskFile;

  public EduEditor(@NotNull final Project project, @NotNull final VirtualFile file) {
    super(project, file, TextEditorProvider.getInstance());
    myTaskFile = EduUtils.getTaskFile(project, file);

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

  public void showLoadingPanel() {
    JBLoadingPanel component = getComponent();
    ((EditorImpl)getEditor()).setViewer(true);
    component.setLoadingText("Loading solution");
    component.startLoading();
  }

  @Override
  public void selectNotify() {
    super.selectNotify();
    PlaceholderDependencyManager.updateDependentPlaceholders(myProject, myTaskFile.getTask());
  }
}
