package com.jetbrains.edu.coursecreator.ui;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.jetbrains.edu.coursecreator.actions.CCCreateCourseArchive;
import com.jetbrains.edu.learning.EduNames;
import com.jetbrains.edu.learning.OpenApiExtKt;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.Vendor;
import com.jetbrains.edu.learning.messages.EduCoreBundle;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentListener;
import java.awt.*;

import static com.jetbrains.edu.learning.marketplace.UtilsKt.addVendor;

public class CCCreateCourseArchivePanel extends JPanel {
  private JPanel myPanel;
  private TextFieldWithBrowseButton myLocationField;
  private JTextField myAuthorField;
  private JLabel myAuthorLabel;

  public CCCreateCourseArchivePanel(@NotNull final Project project, String name, boolean showAuthorField) {
    setLayout(new BorderLayout());
    add(myPanel, BorderLayout.CENTER);
    myAuthorField.setText(getAuthorInitialValue(project));
    myLocationField.setText(getArchiveLocation(project, name));
    FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor();
    myLocationField.addBrowseFolderListener(EduCoreBundle.message("course.creator.create.archive.dialog.title"), null, project, descriptor);
    myAuthorLabel.setVisible(showAuthorField);
    myAuthorField.setVisible(showAuthorField);
  }

  public void addLocationListener(DocumentListener listener) {
    myLocationField.getTextField().getDocument().addDocumentListener(listener);
  }

  public TextFieldWithBrowseButton getLocationField() {
    return myLocationField;
  }

  @NotNull
  private static String getAuthorInitialValue(@NotNull Project project) {
    Course course =  OpenApiExtKt.getCourse(project);
    if (course != null) {
      if (course.getVendor() == null) {
        addVendor(course);
      }
      Vendor vendor = course.getVendor();
      if (vendor != null) {
        return vendor.getName();
      }
    }
    String savedAuthorName = PropertiesComponent.getInstance(project).getValue(CCCreateCourseArchive.AUTHOR_NAME);
    if (savedAuthorName != null) {
      return savedAuthorName;
    }

    String userName = System.getProperty("user.name");
    if (userName != null) {
      return StringUtil.capitalize(userName);
    }
    return "User";
  }

  public String getLocationPath() {
    return myLocationField.getText();
  }

  public String getAuthorName() {
    return myAuthorField.getText();
  }

  private static String getArchiveLocation(@NotNull Project project, String name) {
    String location = PropertiesComponent.getInstance(project).getValue(CCCreateCourseArchive.LAST_ARCHIVE_LOCATION);
    if (location != null) return location;

    String sanitizedName = FileUtil.sanitizeFileName(name);
    if (sanitizedName.startsWith("_")) sanitizedName = EduNames.COURSE;
    return project.getBasePath() + "/" + sanitizedName + ".zip";
  }
}
