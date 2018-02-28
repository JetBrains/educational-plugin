package com.jetbrains.edu.learning.newproject.ui;

import com.intellij.icons.AllIcons;
import com.intellij.ide.impl.ProjectUtil;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.roots.ui.componentsList.components.ScrollablePanel;
import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.BrowserHyperlinkListener;
import com.intellij.ui.FilterComponent;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.panels.HorizontalLayout;
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
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class CoursePanel extends JPanel {

  private static final int HORIZONTAL_MARGIN = 10;
  private static final int LARGE_HORIZONTAL_MARGIN = 25;

  private JBLabel myCourseNameLabel;
  private JPanel myTagsPanel;
  private JEditorPane myInstructorField;
  private JBScrollPane myInfoScroll;
  private JPanel myInfoPanel;
  private JEditorPane myDescriptionPane;

  private AdvancedSettings myAdvancedSettings;
  private JPanel myCourseDescriptionPanel;
  private EduCourseBuilder.LanguageSettings<?> myLanguageSettings;
  @Nullable
  private FilterComponent mySearchField;

  @Nullable
  private LabeledComponent<TextFieldWithBrowseButton> myLocationField;
  private JPanel myCourseNamePanel;

  private String myDescription;

  public CoursePanel(boolean isIndependentPanel, boolean isLocationFieldNeeded, boolean isEditable) {
    createMainPanel(isEditable);
    initUI(isIndependentPanel, isLocationFieldNeeded, isEditable);
  }

  private void createMainPanel(boolean isEditable) {
    createCourseInfoPanel(isEditable);
    myAdvancedSettings = new AdvancedSettings();

    setLayout(new BorderLayout());
    add(myCourseDescriptionPanel, BorderLayout.PAGE_START);
    add(myAdvancedSettings, BorderLayout.PAGE_END);
  }

  private void createCourseInfoPanel(boolean isEditable) {
    myCourseDescriptionPanel= new JPanel(new VerticalFlowLayout());

    createCourseNamePanel(isEditable);

    myTagsPanel = new JPanel(new HorizontalLayout(JBUI.scale(5)));
    myInstructorField = new JEditorPane();

    myDescriptionPane = new JEditorPane();
    myInfoScroll = new JBScrollPane(myDescriptionPane);

    createCourseDescriptionPanel(isEditable);

    myCourseDescriptionPanel.add(myCourseNamePanel);
    myCourseDescriptionPanel.add(myTagsPanel);
    myCourseDescriptionPanel.add(myInstructorField);
    myCourseDescriptionPanel.add(myInfoPanel);
  }

  private void createCourseDescriptionPanel(boolean isEditable) {
    if (isEditable) {
      String rendered = "Rendered";
      String edit = "Edit";
      CardLayout layout = new CardLayout();
      myInfoPanel = new ScrollablePanel(layout);

      JTextArea textArea = new JTextArea();
      textArea.setRows(30);
      textArea.setFont(myInstructorField.getFont());
      MouseAdapter renderListener = editCourseNameListener(() -> {
        myDescription = textArea.getText();
        myDescriptionPane.setText(UIUtil.toHtml(myDescription.replace("\n", "<br>")));
        layout.show(myInfoPanel, rendered);
      });
      JPanel editPanel = createEditPanel(textArea, renderListener, AllIcons.Modules.Edit, BorderLayout.NORTH);
      textArea.addFocusListener(new FocusAdapter() {
        @Override
        public void focusLost(FocusEvent e) {
          myDescription = textArea.getText();
          myDescriptionPane.setText(UIUtil.toHtml(myDescription.replace("\n", "<br>")));
          layout.show(myInfoPanel, rendered);
        }
      });

      textArea.setFont(myInstructorField.getFont());
      MouseAdapter editListener = editCourseNameListener(() -> {
        textArea.setText(myDescription);
        layout.show(myInfoPanel, edit);
      });
      JPanel renderPanel = createEditPanel(myInfoScroll, editListener, AllIcons.Modules.Edit, BorderLayout.NORTH);

      myInfoPanel.add(renderPanel, rendered);
      myInfoPanel.add(editPanel, edit);
      return;
    }

    myInfoPanel = new ScrollablePanel(new BorderLayout());
    myInfoPanel.add(myInfoScroll, BorderLayout.CENTER);
  }

  private void createCourseNamePanel(boolean isEditable) {
    myCourseNameLabel = new JBLabel();
    if (isEditable) {
      String rendered = "Rendered";
      String edit = "Edit";
      CardLayout layout = new CardLayout();
      myCourseNamePanel = new JPanel(layout);


      JTextField textField = new JTextField(myCourseNameLabel.getText());
      MouseAdapter renderListener = editCourseNameListener(() -> {
        myCourseNameLabel.setText(textField.getText());
        layout.show(myCourseNamePanel, rendered);
      });
      JPanel editPanel = createEditPanel(textField, renderListener, AllIcons.Modules.Edit, BorderLayout.SOUTH);
      textField.addFocusListener(new FocusAdapter() {
        @Override
        public void focusLost(FocusEvent e) {
          myCourseNameLabel.setText(textField.getText());
          layout.show(myCourseNamePanel, rendered);
        }
      });

      MouseAdapter editListener = editCourseNameListener(() -> {
        textField.setText(myCourseNameLabel.getText());
        layout.show(myCourseNamePanel, edit);
      });
      JPanel renderedPanel = createEditPanel(myCourseNameLabel, editListener, AllIcons.Modules.Edit, BorderLayout.SOUTH);

      myCourseNamePanel.add(renderedPanel, rendered);
      myCourseNamePanel.add(editPanel, edit);
      return;
    }

    myCourseNamePanel = new JPanel(new BorderLayout());
    myCourseNamePanel.add(myCourseNameLabel, BorderLayout.LINE_START);
  }

  @NotNull
  private static JPanel createEditPanel(JComponent component, MouseAdapter listener, Icon icon, String labelPosition) {
    JPanel doneLabelPanel = createLabel(listener, icon, labelPosition);
    JPanel editPanel = new JPanel(new BorderLayout());
    editPanel.add(component, BorderLayout.CENTER);
    editPanel.add(doneLabelPanel, BorderLayout.WEST);
    return editPanel;
  }

  @NotNull
  private static JPanel createLabel(@NotNull MouseAdapter listener, @NotNull Icon icon, String labelPosition) {
    JPanel doneLabelPanel = new JPanel(new BorderLayout());
    JLabel doneLabel = new JLabel(icon);
    doneLabelPanel.setBorder(JBUI.Borders.empty(0,0, 5, 5));
    doneLabelPanel.add(doneLabel, labelPosition);
    doneLabel.addMouseListener(listener);
    return doneLabelPanel;
  }

  @NotNull
  private static MouseAdapter editCourseNameListener(Runnable onClickAction) {
    return new MouseAdapter() {

      @Override
      public void mouseEntered(MouseEvent e) {
        e.getComponent().setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
      }

      @Override
      public void mouseExited(MouseEvent e) {
        e.getComponent().setCursor(Cursor.getDefaultCursor());
      }

      @Override
      public void mouseClicked(MouseEvent e) {
        onClickAction.run();
      }
    };
  }

  private void initUI(boolean isIndependentPanel, boolean isLocationFieldNeeded, boolean isEditable) {
    int leftMargin;
    if (isIndependentPanel) {
      leftMargin = LARGE_HORIZONTAL_MARGIN;
    } else {
      leftMargin = HORIZONTAL_MARGIN;
    }

    int editableLeftMargin = isEditable ? 0 : leftMargin;
    myCourseNameLabel.setBorder(JBUI.Borders.empty(20, editableLeftMargin, 5, HORIZONTAL_MARGIN));
    Font labelFont = UIUtil.getLabelFont();
    myCourseNameLabel.setFont(new Font(labelFont.getName(), Font.BOLD, JBUI.scaleFontSize(18.0f)));

    myTagsPanel.setBorder(JBUI.Borders.empty(0, leftMargin, 0, HORIZONTAL_MARGIN));

    setTextAreaAttributes(myInstructorField, leftMargin, 15);
    int topMargin = isEditable ? 0 : 15;
    setTextAreaAttributes(myDescriptionPane, editableLeftMargin, topMargin);

    myInfoScroll.setBorder(null);

    myAdvancedSettings.setVisible(false);

    if (isLocationFieldNeeded) {
      myLocationField = createLocationComponent();
    }

    myDescriptionPane.addHyperlinkListener(new BrowserHyperlinkListener());
  }

  public void bindCourse(@NotNull Course course) {
    myDescription = course.getDescription();
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

  public String getCourseName() {
    return myCourseNameLabel.getText();
  }

  public String getDescription() {
    return myDescription;
  }

  public void addLocationFieldDocumentListener(@NotNull DocumentListener listener) {
    if (myLocationField != null) {
      myLocationField.getComponent()
              .getTextField()
              .getDocument()
              .addDocumentListener(listener);
    }
  }

  private static void setTextAreaAttributes(JEditorPane textArea, int leftMargin, int topMargin) {
    textArea.setBorder(JBUI.Borders.empty(topMargin, leftMargin, 10, HORIZONTAL_MARGIN));
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
    myDescriptionPane.setText(htmlDescription(course));
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
          UIUtil.setCursor(tagComponent, Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
      });
    }
    return tagComponent;
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
