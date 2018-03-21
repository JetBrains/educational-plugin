package com.jetbrains.edu.coursecreator.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.InputValidatorEx;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.DocumentAdapter;
import com.jetbrains.edu.coursecreator.CCUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;

public class CCCreateStudyItemDialog extends DialogWrapper {
  private final CCCreateStudyItemPanel myPanel;
  private final InputValidatorEx myValidator;

  public CCCreateStudyItemDialog(@Nullable Project project,
                                 String itemName,
                                 String thresholdName,
                                 int thresholdIndex,
                                 @NotNull VirtualFile parent) {
    super(project);
    myPanel = new CCCreateStudyItemPanel(itemName, thresholdName, thresholdIndex);
    setTitle("Create New " + StringUtil.toTitleCase(itemName));
    init();
    myValidator = new CCUtils.PathInputValidator(parent);
    myPanel.getNameField().getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      protected void textChanged(DocumentEvent e) {
        initValidation();
      }
    });
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
   return myPanel;
  }

  public String getName() {
    return myPanel.getItemName();
  }

  public int getIndexDelta() {
    return myPanel.getIndexDelta();
  }

  @Nullable
  @Override
  protected ValidationInfo doValidate() {
    String name = myPanel.getItemName();
    if (name == null) {
      return new ValidationInfo("empty name");
    }
    if (myValidator.checkInput(name)) {
      return null;
    }
    String errorText = myValidator.getErrorText(name);
    return errorText == null ? null : new ValidationInfo(errorText);
  }
}
