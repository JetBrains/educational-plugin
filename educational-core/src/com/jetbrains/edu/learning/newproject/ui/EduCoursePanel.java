package com.jetbrains.edu.learning.newproject.ui;

import com.android.annotations.NonNull;
import com.intellij.ide.impl.ProjectUtil;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.openapi.ui.OnePixelDivider;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.jetbrains.edu.learning.EduPluginConfigurator;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.newproject.EduCourseProjectGenerator;
import com.jetbrains.edu.learning.stepic.StepicUser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class EduCoursePanel extends JPanel {

  private static final Color COLOR = new Color(70, 130, 180, 70);

  private JPanel myCoursePanel;
  private EduAdvancedSettings myAdvancedSettings;
  private JEditorPane myDescriptionTextArea;
  private JBLabel myCourseNameLabel;
  private JPanel myTagsPanel;
  private JBScrollPane myInfoScroll;

  @Nullable
  private LabeledComponent<TextFieldWithBrowseButton> myLocationField;

  // Used in `EduCoursesPanel` in initializing code generated for form
  @SuppressWarnings("unused")
  public EduCoursePanel() {
    this(true);
  }

  public EduCoursePanel(boolean isLocationFieldNeeded) {
    setLayout(new BorderLayout());
    add(myCoursePanel, BorderLayout.CENTER);
    initUI(isLocationFieldNeeded);
  }

  private void initUI(boolean isLocationFieldNeeded) {
    myCourseNameLabel.setBorder(JBUI.Borders.empty(20, 10, 5, 10));
    Font labelFont = UIUtil.getLabelFont();
    myCourseNameLabel.setFont(new Font(labelFont.getName(), Font.BOLD, JBUI.scaleFontSize(18.0f)));
    myTagsPanel.setBorder(JBUI.Borders.empty(0, 10));
    myDescriptionTextArea.setBorder(JBUI.Borders.empty(20, 10, 10, 10));
    myDescriptionTextArea.setEditorKit(UIUtil.getHTMLEditorKit());
    myDescriptionTextArea.setEditable(false);
    myDescriptionTextArea.setPreferredSize(JBUI.size(myCoursePanel.getPreferredSize()));
    myInfoScroll.setBorder(null);

    Border border = JBUI.Borders.customLine(OnePixelDivider.BACKGROUND, 1, 0, 1, 1);
    myCoursePanel.setBorder(border);

    myDescriptionTextArea.setBackground(UIUtil.getPanelBackground());
    myAdvancedSettings.setVisible(false);

    if (isLocationFieldNeeded) {
      myLocationField = createLocationComponent();
    }
  }

  public void bindCourse(@NotNull Course course) {
    updateCourseDescriptionPanel(course);
    updateAdvancedSettings(course);
  }

  public void clearContent() {
    myInfoScroll.setVisible(false);
    myAdvancedSettings.setVisible(false);
  }

  @Nullable
  public String getLocationString() {
    return myLocationField == null ? null : myLocationField.getComponent().getText();
  }

  private void updateCourseDescriptionPanel(@NotNull Course course) {
    myInfoScroll.setVisible(true);
    myCourseNameLabel.setText(course.getName());
    myDescriptionTextArea.setText(htmlDescription(course));
    updateTags(course);
  }

  @NotNull
  private String htmlDescription(@NotNull Course course) {
    StringBuilder builder = new StringBuilder();
    List<StepicUser> authors = course.getAuthors();
    if (!authors.isEmpty()) {
      builder.append("<b>Instructor");
      if (authors.size() > 1) {
        builder.append("s");
      }
      builder.append("</b>: ");
      List<String> fullNames = CourseUtils.getAuthorFullNames(course);
      builder.append(StringUtil.join(fullNames, ", "));
      builder.append("<br><br>");
    }
    String description = course.getDescription() != null ? course.getDescription() : "";
    builder.append(description.replace("\n", "<br>"));
    return UIUtil.toHtml(builder.toString());
  }

  private void updateTags(@NotNull Course course) {
    myTagsPanel.removeAll();
    addTags(myTagsPanel, course);
    myTagsPanel.revalidate();
    myTagsPanel.repaint();
  }

  private void updateAdvancedSettings(@NotNull Course course) {
    if (myLocationField != null) {
      myLocationField.getComponent().setText(nameToLocation(course.getName()));
    }
    EduPluginConfigurator configurator = EduPluginConfigurator.INSTANCE.forLanguage(course.getLanguageById());
    if (configurator == null) {
      return;
    }
    EduCourseProjectGenerator generator = configurator.getEduCourseProjectGenerator();
    if (generator == null) {
      return;
    }

    List<LabeledComponent> settingsComponents = new ArrayList<>();
    if (myLocationField != null) {
      settingsComponents.add(myLocationField);
    }
    LabeledComponent<JComponent> component = generator.getLanguageSettingsComponent(course);
    if (component != null) {
      settingsComponents.add(component);
    }

    if (settingsComponents.isEmpty()) {
      myAdvancedSettings.setVisible(false);
    } else {
      myAdvancedSettings.setVisible(true);
      myAdvancedSettings.setSettingsComponents(settingsComponents);
    }
  }

  @NonNull
  private static LabeledComponent<TextFieldWithBrowseButton> createLocationComponent() {
    TextFieldWithBrowseButton field = new TextFieldWithBrowseButton();
    field.addBrowseFolderListener("Select Course Location", "Select course location", null,
            FileChooserDescriptorFactory.createSingleFolderDescriptor());
    return LabeledComponent.create(field, "Location", BorderLayout.WEST);
  }

  @NotNull
  private static String nameToLocation(@NotNull String courseName) {
    String name = FileUtil.sanitizeFileName(courseName);
    return FileUtil.findSequentNonexistentFile(new File(ProjectUtil.getBaseDir()), name, "").getAbsolutePath();
  }

  private static void addTags(JPanel tagsPanel, @NotNull Course course) {
    for (String tag : CourseUtils.getTags(course)) {
      tagsPanel.add(createTagLabel(tag));
    }
  }

  @NonNull
  private static JLabel createTagLabel(String tagText) {
    Border emptyBorder = JBUI.Borders.empty(3, 5);
    JBLabel label = new JBLabel(tagText);
    label.setOpaque(true);
    label.setBorder(emptyBorder);
    label.setBackground(new JBColor(COLOR, COLOR));
    return label;
  }
}
