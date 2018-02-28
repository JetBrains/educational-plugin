package com.jetbrains.edu.learning.newproject.ui;

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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CoursePanel extends JPanel {

  private static final int HORIZONTAL_MARGIN = 10;
  private static final int LARGE_HORIZONTAL_MARGIN = 25;

  private static final String RENDERED_VIEW = "Rendered";
  private static final String EDIT_VIEW = "Edit";

  private JPanel myMainPanel;

  private JLabel myEditLabel;

  private AdvancedSettings myAdvancedSettings;
  @Nullable private FilterComponent mySearchField;


  @Nullable private LabeledComponent<TextFieldWithBrowseButton> myLocationField;
  private String myDescription;
  private RenderedViewPanel myRenderedViewPanel;

  public CoursePanel(boolean isIndependentPanel, boolean isLocationFieldNeeded, boolean isEditable) {
    myAdvancedSettings = new AdvancedSettings();
    if (isEditable) {
      CardLayout layout = new CardLayout();
      setLayout(layout);
      EditViewPanel editViewPanel = new EditViewPanel();
      myRenderedViewPanel = new RenderedViewPanel(isIndependentPanel, isLocationFieldNeeded);
      JPanel cardsPanel = new JPanel(layout);
      cardsPanel.add(editViewPanel, EDIT_VIEW);
      cardsPanel.add(myRenderedViewPanel, RENDERED_VIEW);
      layout.show(cardsPanel, RENDERED_VIEW);
      JPanel editLabelPanel = createEditLabelPanel(cardsPanel, layout, editViewPanel, myRenderedViewPanel);

      myMainPanel = new JPanel(new BorderLayout());
      myMainPanel.add(editLabelPanel, BorderLayout.PAGE_START);
      myMainPanel.add(cardsPanel, BorderLayout.CENTER);
    }
    else {
      myRenderedViewPanel = new RenderedViewPanel(isIndependentPanel, isLocationFieldNeeded);
      myMainPanel = new JPanel(new BorderLayout());
      myMainPanel.add(myRenderedViewPanel, BorderLayout.CENTER);
    }

    setLayout(new BorderLayout());
    add(myMainPanel, BorderLayout.CENTER);
    myMainPanel.setVisible(false);
  }

  private JPanel createEditLabelPanel(JPanel cardsPanel, CardLayout layout,
                                    EditViewPanel editViewPanel,
                                    RenderedViewPanel renderedViewPanel) {
    JPanel editLabelPanel = new JPanel(new BorderLayout());
    String editText = "Edit";
    String doneText = "Done";

    myEditLabel = new JLabel(UIUtil.toHtml(wrapLabelText(editText)));
    editLabelPanel.add(myEditLabel, BorderLayout.EAST);
    myEditLabel.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        boolean toEditMode = myEditLabel.getText().contains(editText);
        if (toEditMode) {
          myEditLabel.setText(UIUtil.toHtml(wrapLabelText(doneText)));
          layout.show(cardsPanel, EDIT_VIEW);
          editViewPanel.setAllFields(renderedViewPanel.getCourseName(), renderedViewPanel.myTagsPanel.getTags(),
                                     renderedViewPanel.getAuthor(), renderedViewPanel.getLanguage(),
                                     renderedViewPanel.getDescription());
        }
        else {
          myEditLabel.setText(UIUtil.toHtml(wrapLabelText(editText)));
          layout.show(cardsPanel, RENDERED_VIEW);
          renderedViewPanel
            .setAllFields(editViewPanel.getCourseName(), editViewPanel.getTags(), editViewPanel.getAuthor(), editViewPanel.getLanguage(),
                          editViewPanel.getDescription());
        }
      }

      @Override
      public void mouseEntered(MouseEvent e) {
        e.getComponent().setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
      }

      @Override
      public void mouseExited(MouseEvent e) {
        e.getComponent().setCursor(Cursor.getDefaultCursor());
      }
    });

    return editLabelPanel;
  }

  @NotNull
  private static String wrapLabelText(String text) {
    return "<u><i>" + text+ "</i></u>";
  }

  private class EditViewPanel extends JPanel {
    JTextField courseNameField = new JTextField();
    JTextField tagsField = new JTextField();
    JTextField authorField = new JTextField();
    JTextField humanLanguageField = new JTextField();
    JTextField descriptionArea = new JTextField();

    public EditViewPanel() {
      setLayout(new VerticalFlowLayout());
      setAllFields("", Collections.emptyList(), "", "", "");
      initUI();
    }

    public void setAllFields(String courseName, List<Tag> tags, String author, String language, String description) {
      courseNameField.setText(courseName);
      tagsField.setText(StringUtil.join(tags, ", "));
      authorField.setText(author);
      humanLanguageField.setText(language);
      descriptionArea.setText(description);
      initUI();
    }

    private void initUI() {
      courseNameField.setBorder(JBUI.Borders.empty(20, HORIZONTAL_MARGIN, 5, HORIZONTAL_MARGIN));
      tagsField.setBorder(JBUI.Borders.empty(0, HORIZONTAL_MARGIN));

      int TOP_MARGIN = 15;
      authorField.setBorder(JBUI.Borders.empty(TOP_MARGIN, HORIZONTAL_MARGIN, 10, HORIZONTAL_MARGIN));

      descriptionArea.setBorder(null);

      myAdvancedSettings.setVisible(false);

      myLocationField = createLocationComponent();
    }

    public String getCourseName() {
      return courseNameField.getText();
    }

    public List<Tag> getTags() {
      ArrayList<Tag> tags = new ArrayList<>();
      for (String tagName: tagsField.getText().split(", ")) {
        tags.add(new Tag(tagName));
      }

      return tags;
    }

    public String getAuthor() {
      return authorField.getText();
    }

    public String getLanguage() {
      return humanLanguageField.getText();
    }

    public String getDescription() {
      return myDescription;
    }
  }

  private class RenderedViewPanel extends JPanel {
    private JBLabel myCourseNameLabel;
    private TagsPanel myTagsPanel;
    private JLabel myHumanLanguage;
    private JEditorPane myInstructorField;
    private JBScrollPane myInfoScroll;
    private JPanel myInfoPanel;
    private JEditorPane myDescriptionPane;
    private EduCourseBuilder.LanguageSettings<?> myLanguageSettings;
    private String myRawLanguage;
    private String myRawDescription;

    boolean isIndependentPanel;
    boolean isLocationFieldNeeded;

    public RenderedViewPanel(boolean isIndependentPanel, boolean isLocationFieldNeeded) {
      this.isIndependentPanel = isIndependentPanel;
      this.isLocationFieldNeeded = isLocationFieldNeeded;

      setLayout(new VerticalFlowLayout());

      myCourseNameLabel = new JBLabel();

      myTagsPanel = new TagsPanel();
      myInstructorField = new JEditorPane();
      myHumanLanguage = new JLabel();

      myDescriptionPane = new JEditorPane();
      myInfoScroll = new JBScrollPane(myDescriptionPane);
      myInfoPanel = new ScrollablePanel(new BorderLayout());
      myInfoPanel.add(myInfoScroll, BorderLayout.CENTER);

      add(myCourseNameLabel);
      add(myTagsPanel);
      add(myHumanLanguage);
      add(myInstructorField);
      add(myInfoPanel);

      initUI(isIndependentPanel, isLocationFieldNeeded);
    }

    public void setAllFields(String courseName, List<Tag> tags, String instructorText, String language, String description) {
      myCourseNameLabel.setText(courseName);
      myTagsPanel.updateTags(tags);

      if (instructorText == null) {
        myInstructorField.setPreferredSize(JBUI.size(0, 0));
      } else {
        myInstructorField.setPreferredSize(null);
        myInstructorField.setText(instructorText);
      }
      myRawLanguage = language;
      myRawDescription = description;
      myHumanLanguage.setText(UIUtil.toHtml("<b>Language<b> " + myRawLanguage));
      myDescriptionPane.setText(htmlDescription(description));

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
      setTextAreaAttributes(myDescriptionPane, leftMargin);

      myInfoScroll.setBorder(null);

      myAdvancedSettings.setVisible(false);

      if (isLocationFieldNeeded) {
        myLocationField = createLocationComponent();
      }

      myDescriptionPane.addHyperlinkListener(new BrowserHyperlinkListener());
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

    private void updateCourseDescriptionPanel(@NotNull Course course) {
      String instructorText = htmlInstructorText(course);
      setAllFields(course.getName(), course.getTags(), instructorText,
                   course.getHumanLanguage(),
                   course.getDescription() != null ? course.getDescription() : "");
    }

    public Object getSettings() {
      return myLanguageSettings.getSettings();
    }

    public String getCourseName() {
      return myCourseNameLabel.getText();
    }

    public String getAuthor() {
      return myInstructorField.getText();
    }

    public String getLanguage() {
      return myRawLanguage;
    }

    public String getDescription() {
      return myRawDescription;
    }
  }

  private class TagsPanel extends JPanel {
    private List<Tag> myTags = new ArrayList<>();


    public TagsPanel() {
      setLayout(new HorizontalLayout(JBUI.scale(5)));
    }

    private void addTags(List<Tag> tags) {
      for (Tag tag : tags) {
        add(createTagLabel(tag));
      }
    }

    private void updateTags(List<Tag> tags) {
      myTags = tags;
      removeAll();
      addTags(tags);
      revalidate();
      repaint();
    }

    public List<Tag> getTags() {
      return myTags;
    }
  }

  public void bindCourse(@NotNull Course course) {
    myDescription = course.getDescription();
    myMainPanel.setVisible(true);
    myRenderedViewPanel.updateCourseDescriptionPanel(course);
    myRenderedViewPanel.updateAdvancedSettings(course);
  }

  public void hideContent() {
    myMainPanel.setVisible(false);
  }

  @Nullable
  public String getLocationString() {
    return myLocationField == null ? null : myLocationField.getComponent().getText();
  }

  public Object getProjectSettings() {
    return myRenderedViewPanel.getSettings();
  }

  public String getCourseName() {
    return myRenderedViewPanel.getCourseName();
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

  private static void setTextAreaAttributes(JEditorPane textArea, int leftMargin) {
    textArea.setBorder(JBUI.Borders.empty(15, leftMargin, 10, HORIZONTAL_MARGIN));
    textArea.setEditorKit(UIUtil.getHTMLEditorKit());
    textArea.setEditable(false);
    textArea.setBackground(UIUtil.getPanelBackground());
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
  private static String htmlDescription(String description) {
    return UIUtil.toHtml(description.replace("\n", "<br>"));
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
