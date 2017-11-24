package com.jetbrains.edu.learning.newproject.ui;

import com.intellij.ide.impl.ProjectUtil;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.openapi.ui.OnePixelDivider;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.BrowserHyperlinkListener;
import com.intellij.ui.FilterComponent;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.PathUtil;
import com.intellij.util.io.IOUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.jetbrains.edu.learning.EduConfigurator;
import com.jetbrains.edu.learning.EduConfiguratorManager;
import com.jetbrains.edu.learning.EduCourseBuilder;
import com.jetbrains.edu.learning.EduNames;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.Tag;
import com.jetbrains.edu.learning.stepik.StepicUser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class CoursePanel extends JPanel {

  private static final int HORIZONTAL_MARGIN = 10;
  private static final int LARGE_HORIZONTAL_MARGIN = 15;

  private JPanel myCoursePanel;
  private JBLabel myCourseNameLabel;
  private JPanel myTagsPanel;
  private JEditorPane myInstructorField;
  private JBScrollPane myInfoScroll;
  private JEditorPane myDescriptionTextArea;

  private AdvancedSettings myAdvancedSettings;
  private JPanel myCourseDescriptionPanel;
  private EduCourseBuilder.LanguageSettings<?> myLanguageSettings;
  @Nullable
  private FilterComponent mySearchField;

  @Nullable
  private LabeledComponent<TextFieldWithBrowseButton> myLocationField;

  public CoursePanel(boolean isIndependentPanel, boolean isLocationFieldNeeded) {
    setLayout(new BorderLayout());
    add(myCoursePanel, BorderLayout.CENTER);
    initUI(isIndependentPanel, isLocationFieldNeeded);
  }

  private void initUI(boolean isIndependentPanel, boolean isLocationFieldNeeded) {
    int leftMargin;
    if (isIndependentPanel) {
      leftMargin = LARGE_HORIZONTAL_MARGIN;
    } else {
      leftMargin = HORIZONTAL_MARGIN;
    }

    myCourseNameLabel.setBorder(JBUI.Borders.empty(20, leftMargin, 5, HORIZONTAL_MARGIN));
    Font labelFont = UIUtil.getLabelFont();
    myCourseNameLabel.setFont(new Font(labelFont.getName(), Font.BOLD, JBUI.scaleFontSize(18.0f)));

    myTagsPanel.setBorder(JBUI.Borders.empty(0, leftMargin, 0, HORIZONTAL_MARGIN));

    setTextAreaAttributes(myInstructorField, leftMargin);
    setTextAreaAttributes(myDescriptionTextArea, leftMargin);

    myInfoScroll.setBorder(null);

    // We want to show left part of border only if panel is independent
    int leftBorder = isIndependentPanel ? 1 : 0;
    Border border = JBUI.Borders.customLine(OnePixelDivider.BACKGROUND, 1, leftBorder, 1, 1);
    myCoursePanel.setBorder(border);

    myAdvancedSettings.setVisible(false);

    if (isLocationFieldNeeded) {
      myLocationField = createLocationComponent();
    }

    myDescriptionTextArea.addHyperlinkListener(new BrowserHyperlinkListener());
  }

  public void bindCourse(@NotNull Course course) {
    myCourseDescriptionPanel.setVisible(true);
    updateCourseDescriptionPanel(course);
    updateAdvancedSettings(course);
  }

  public void hideContent() {
    myCourseDescriptionPanel.setVisible(false);
  }

  @Nullable
  public String getLocationString() {
    return myLocationField == null ? null : myLocationField.getComponent().getText();
  }

  public Object getProjectSettings() {
    return myLanguageSettings.getSettings();
  }

  public void addLocationFieldDocumentListener(@NotNull DocumentListener listener) {
    if (myLocationField != null) {
      myLocationField.getComponent()
              .getTextField()
              .getDocument()
              .addDocumentListener(listener);
    }
  }

  private static void setTextAreaAttributes(JEditorPane textArea, int leftMargin) {
    textArea.setBorder(JBUI.Borders.empty(15, leftMargin, 10, HORIZONTAL_MARGIN));
    textArea.setEditorKit(UIUtil.getHTMLEditorKit());
    textArea.setEditable(false);
    textArea.setBackground(UIUtil.getPanelBackground());
  }

  private void updateCourseDescriptionPanel(@NotNull Course course) {
    myCourseNameLabel.setText(course.getName());
    updateTags(course);
    String instructorText = htmlInstructorText(course);
    if (instructorText == null) {
      myInstructorField.setPreferredSize(JBUI.size(0, 0));
    } else {
      myInstructorField.setPreferredSize(null);
      myInstructorField.setText(instructorText);
    }
    myDescriptionTextArea.setText(htmlDescription(course));
  }

  @Nullable
  private static String htmlInstructorText(@NotNull Course course) {
    StringBuilder builder = new StringBuilder();
    List<StepicUser> authors = course.getAuthors();
    if (authors.isEmpty()) return null;
    builder.append("<b>Instructor");
    if (authors.size() > 1) {
      builder.append("s");
    }
    builder.append("</b>: ");
    builder.append(StringUtil.join(course.getAuthorFullNames(), ", "));
    return UIUtil.toHtml(builder.toString());
  }

  @NotNull
  private static String htmlDescription(@NotNull Course course) {
    String description = course.getDescription() != null ? course.getDescription() : "";
    return UIUtil.toHtml(description.replace("\n", "<br>"));
  }

  private void updateTags(@NotNull Course course) {
    myTagsPanel.removeAll();
    addTags(myTagsPanel, course);
    myTagsPanel.revalidate();
    myTagsPanel.repaint();
  }

  private void updateAdvancedSettings(@NotNull Course course) {
    if (myLocationField != null) {
      myLocationField.getComponent().setText(nameToLocation(course));
    }
    EduConfigurator configurator = EduConfiguratorManager.forLanguage(course.getLanguageById());
    if (configurator == null) {
      return;
    }
    myLanguageSettings = configurator.getCourseBuilder().getLanguageSettings();

    List<LabeledComponent> settingsComponents = new ArrayList<>();
    if (myLocationField != null) {
      settingsComponents.add(myLocationField);
    }
    LabeledComponent<JComponent> component = myLanguageSettings.getLanguageSettingsComponent(course);
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

  @NotNull
  private static LabeledComponent<TextFieldWithBrowseButton> createLocationComponent() {
    TextFieldWithBrowseButton field = new TextFieldWithBrowseButton();
    field.addBrowseFolderListener("Select Course Location", "Select course location", null,
            FileChooserDescriptorFactory.createSingleFolderDescriptor());
    return LabeledComponent.create(field, "Location", BorderLayout.WEST);
  }

  @NotNull
  private static String nameToLocation(@NotNull Course course) {
    final String courseName = course.getName();
    final String language = course.getLanguageById().getDisplayName();
    final String humanLanguage = course.getHumanLanguage();
    String name = courseName;
    if (!IOUtil.isAscii(name)) {
      //there are problems with venv creation for python course
      name = StringUtil.capitalize(EduNames.COURSE + " " + language + " " + humanLanguage);
    }
    if (!PathUtil.isValidFileName(name)) {
      name = FileUtil.sanitizeFileName(name);
    }

    return FileUtil.findSequentNonexistentFile(new File(ProjectUtil.getBaseDir()), name, "").getAbsolutePath();
  }

  private void addTags(JPanel tagsPanel, @NotNull Course course) {
    for (Tag tag : course.getTags()) {
      tagsPanel.add(createTagLabel(tag));
    }
  }

  @NotNull
  private JComponent createTagLabel(Tag tag) {
    JComponent tagComponent = tag.createComponent(isTagSelected(tag));
    if (mySearchField != null) {
      tagComponent.addMouseListener(new MouseAdapter() {
        @Override
        public void mousePressed(MouseEvent e) {
          if (!isTagSelected(tag)) {
            mySearchField.getTextEditor().setText(tag.getSearchText());
            return;
          }
          mySearchField.getTextEditor().setText("");
        }

        @Override
        public void mouseEntered(MouseEvent e) {
          setCursor(tagComponent, Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
      });
    }
    return tagComponent;
  }

  // Copied from new IDEA version codebase.
  // TODO: drop it after update to 2017.2 or newer and use 'UIUtil.setCursor'
  private static void setCursor(Component component, Cursor cursor) {
    // cursor is updated by native code even if component has the same cursor, causing performance problems (IDEA-167733)
    if (component.isCursorSet() && component.getCursor() == cursor) return;
    component.setCursor(cursor);
  }

  private boolean isTagSelected(@NotNull Tag tag) {
    if (mySearchField == null || mySearchField.getFilter().isEmpty()) {
      return false;
    }
    for (String filterPart : CoursesPanel.getFilterParts(mySearchField.getFilter())) {
      if (tag.getSearchText().equals(filterPart)) {
        return true;
      }
    }
    return false;

  }

  public void bindSearchField(@NotNull FilterComponent searchField) {
    mySearchField = searchField;
  }
}
