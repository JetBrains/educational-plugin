package com.jetbrains.edu.coursecreator.actions.placeholder;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class CCCreateAnswerPlaceholderDialog extends DialogWrapper {

  private static final String TITLE = "Add Answer Placeholder";
  private final CCCreateAnswerPlaceholderPanel myPanel;
  private final Project myProject;

  public Project getProject() {
    return myProject;
  }

  public CCCreateAnswerPlaceholderDialog(@NotNull final Project project, String placeholderText) {
    super(project, true);
    
    myProject = project;
    myPanel = new CCCreateAnswerPlaceholderPanel(placeholderText);
    setTitle(TITLE);
    init();
    initValidation();
  }

  @NotNull
  public String getTaskText() {
    return StringUtil.notNullize(myPanel.getAnswerPlaceholderText()).trim();
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    return myPanel.getMailPanel();
  }

  @Nullable
  @Override
  public ValidationInfo doValidate() {
    return null;
  }

  @Nullable
  @Override
  public JComponent getPreferredFocusedComponent() {
    return myPanel.getPreferredFocusedComponent();
  }
}
