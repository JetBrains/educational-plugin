package com.jetbrains.edu.coursecreator.ui;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.ui.JBUI;
import com.jetbrains.edu.coursecreator.actions.CreateCourseArchiveAction;
import com.jetbrains.edu.learning.EduNames;
import com.jetbrains.edu.learning.EduSettings;
import com.jetbrains.edu.learning.newproject.ui.ErrorComponent;
import com.jetbrains.edu.learning.newproject.ui.ValidationMessage;
import com.jetbrains.edu.learning.newproject.ui.ValidationMessageType;
import com.jetbrains.edu.learning.stepik.StepikUser;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentListener;
import java.awt.*;

public class CCCreateCourseArchivePanel extends JPanel {
  private JPanel myPanel;
  private TextFieldWithBrowseButton myLocationField;
  private ErrorComponent myErrorComponent;
  private JTextField myAuthorField;
  private JLabel myAuthorLabel;

  public CCCreateCourseArchivePanel(@NotNull final Project project, String name, boolean showAuthorField) {
    setLayout(new BorderLayout());
    add(myPanel, BorderLayout.CENTER);
    setErrorVisible(false);
    myAuthorField.setText(getAuthorInitialValue(project));
    myLocationField.setText(getArchiveLocation(project, name));
    FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor();
    myLocationField.addBrowseFolderListener("Choose Location Folder", null, project, descriptor);
    myAuthorLabel.setVisible(showAuthorField);
    myAuthorField.setVisible(showAuthorField);
    myErrorComponent.setBorder(JBUI.Borders.empty(2, 1));
  }

  public void addLocationListener(DocumentListener listener) {
    myLocationField.getTextField().getDocument().addDocumentListener(listener);
  }

  @NotNull
  private static String getAuthorInitialValue(@NotNull Project project) {
    String savedAuthorName = PropertiesComponent.getInstance(project).getValue(CreateCourseArchiveAction.AUTHOR_NAME);
    if (savedAuthorName != null) {
      return savedAuthorName;
    }
    StepikUser stepikUser = EduSettings.getInstance().getUser();
    if (stepikUser != null) {
      return stepikUser.getName();
    }

    String userName = System.getProperty("user.name");
    if (userName != null) {
      return StringUtil.capitalize(userName);
    }
    return "User";
  }

  protected void setErrorVisible(boolean isVisible) {
    myErrorComponent.setVisible(isVisible);
  }

  protected void setError() {
    myErrorComponent.setErrorMessage(new ValidationMessage("Invalid location. File already exists.",
                                                           "",
                                                           "",
                                                           null,
                                                           ValidationMessageType.ERROR));
    setErrorVisible(true);
  }

  public String getLocationPath() {
    return myLocationField.getText();
  }

  public String getAuthorName() {
    return myAuthorField.getText();
  }

  private static String getArchiveLocation(@NotNull Project project, String name) {
    String location = PropertiesComponent.getInstance(project).getValue(CreateCourseArchiveAction.LAST_ARCHIVE_LOCATION);
    if (location != null) return location;

    String sanitizedName = FileUtil.sanitizeFileName(name);
    if (sanitizedName.startsWith("_")) sanitizedName = EduNames.COURSE;
    return project.getBasePath() + "/" + sanitizedName + ".zip";
  }
}
