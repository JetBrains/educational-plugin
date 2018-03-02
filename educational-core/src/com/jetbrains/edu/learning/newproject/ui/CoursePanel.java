package com.jetbrains.edu.learning.newproject.ui;

import com.intellij.ide.impl.ProjectUtil;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.roots.ui.componentsList.components.ScrollablePanel;
import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.*;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.panels.HorizontalLayout;
import com.intellij.util.ObjectUtils;
import com.intellij.util.PathUtil;
import com.intellij.util.io.IOUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.jetbrains.edu.learning.*;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.HumanLanguageTag;
import com.jetbrains.edu.learning.courseFormat.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CoursePanel extends JPanel {

  private static final int HORIZONTAL_MARGIN = 10;
  private static final int LARGE_HORIZONTAL_MARGIN = 25;
  private static final JBColor EDIT_VIEW_BACKGROUND = new JBColor(Gray._252, new Color(49, 52, 53));

  private static final String RENDERED_VIEW = "Rendered";
  private static final String EDIT_VIEW = "Edit";
  public static final String EDIT_LABEL_TEXT = "Edit";
  public static final String DONE_LABEL_TEXT = "Render";

  private JPanel myMainPanel;

  private JLabel myEditLabel;

  private AdvancedSettings myAdvancedSettings;
  @Nullable private FilterComponent mySearchField;


  @Nullable private LabeledComponent<TextFieldWithBrowseButton> myLocationField;
  private RenderedViewPanel myRenderedViewPanel;
  private EditViewPanel myEditViewPanel;
  private List<ValidationListener> myValidationListeners = new ArrayList<>();
  private Course myCourse;

  public CoursePanel(boolean isIndependentPanel, boolean isLocationFieldNeeded, boolean isEditable) {
    myAdvancedSettings = new AdvancedSettings();
    if (isEditable) {
      CardLayout layout = new CardLayout();
      setLayout(layout);
      myEditViewPanel = new EditViewPanel();
      myRenderedViewPanel = new RenderedViewPanel(isIndependentPanel, isLocationFieldNeeded);
      JPanel cardsPanel = new JPanel(layout);
      cardsPanel.add(myEditViewPanel, EDIT_VIEW);
      cardsPanel.add(myRenderedViewPanel, RENDERED_VIEW);
      layout.show(cardsPanel, RENDERED_VIEW);
      JPanel editLabelPanel = createEditLabelPanel(cardsPanel, layout, myEditViewPanel, myRenderedViewPanel);

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

    myEditLabel = new JLabel(UIUtil.toHtml(wrapLabelText(EDIT_LABEL_TEXT)));
    myEditLabel.setBorder(JBUI.Borders.empty(10, 0, 0, 10));
    editLabelPanel.add(myEditLabel, BorderLayout.EAST);
    myEditLabel.addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent e) {
        if (!myEditLabel.isEnabled()) return;

        boolean toEditMode = isEditingMode();
        if (toEditMode) {
          myEditLabel.setText(UIUtil.toHtml(wrapLabelText(DONE_LABEL_TEXT)));
          layout.show(cardsPanel, EDIT_VIEW);
          editViewPanel.setAllFields(renderedViewPanel.getCourseName(), renderedViewPanel.getAuthorsString(), renderedViewPanel.getLanguage(),
                                     renderedViewPanel.getDescription());
        }
        else {
          myEditLabel.setText(UIUtil.toHtml(wrapLabelText(EDIT_LABEL_TEXT)));
          layout.show(cardsPanel, RENDERED_VIEW);
          renderedViewPanel
            .setAllFields(editViewPanel.getCourseName(), null, editViewPanel.getAuthor(), editViewPanel.getLanguage(),
                          editViewPanel.getDescription());
        }
      }

      @Override
      public void mouseEntered(MouseEvent e) {
        if (!myEditLabel.isEnabled()) return;
        e.getComponent().setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
      }

      @Override
      public void mouseExited(MouseEvent e) {
        if (!myEditLabel.isEnabled()) return;
        e.getComponent().setCursor(Cursor.getDefaultCursor());
      }
    });

    return editLabelPanel;
  }

  private boolean isEditingMode() {
    return myEditLabel.getText().contains(EDIT_LABEL_TEXT);
  }

  public void applyChanges(@NotNull Course myCourse) {
    if (isEditingMode()) {
      myEditViewPanel.applyChanges(myCourse);
    }
    else {
      myRenderedViewPanel.applyChanges(myCourse);
    }
  }

  public void addValidationListener(ValidationListener validationListener) {
    myValidationListeners.add(validationListener);
  }

  @NotNull
  private static String wrapLabelText(String text) {
    return "<u><i>" + text+ "</i></u>";
  }

  private class EditViewPanel extends JPanel {
    public static final int ARC_SIZE = 10;
    private final JBLabel myCourseNameLabel = new JBLabel("Course name:");
    private final JBLabel myInstructorLabel = new JBLabel("Instructor: ");
    private final JBLabel myLanguageLabel = new JBLabel("Language: ");
    private final JBLabel myDescriptionLabel = new JBLabel("Description: ");


    private final JTextField myCourseNameField = new JTextField();
    private final JTextField myInstructorField = new JTextField();
    private final JTextField myHumanLanguageField = new JTextField();
    private final JTextArea myDescriptionArea = new JTextArea();
    private final JLabel myErrorLabel = new JBLabel();

    public EditViewPanel() {
      VerticalFlowLayout flowLayout = new VerticalFlowLayout();
      flowLayout.setVerticalFill(true);
      setLayout(flowLayout);

      setAllFields("", "", "", "");

      add(myCourseNameLabel);
      add(myCourseNameField);

      add(myInstructorLabel);
      add(myInstructorField);

      add(myLanguageLabel);
      add(myHumanLanguageField);

      add(myDescriptionLabel);
      JBScrollPane scrollPane = new JBScrollPane(myDescriptionArea);
      scrollPane.setBorder(null);
      add(scrollPane);

      JPanel panel = new JPanel(new BorderLayout());
      panel.add(myErrorLabel, BorderLayout.WEST);
      add(panel);

      setupValidation();
      addValidationListener(new ValidationListener() {
        @Override
        public void onInputDataValidated(boolean isInputDataComplete) {
          myEditLabel.setEnabled(isInputDataComplete);
        }
      });
    }

    public void setAllFields(String courseName, String author, String language, String description) {
      initUI();
      myCourseNameField.setText(courseName);
      myInstructorField.setText(author);
      myHumanLanguageField.setText(language);
      myDescriptionArea.setText(description);
    }

    private void initUI() {
      int topLabelOffset = 10;
      int bottomLabelOffset = 2;

      myCourseNameLabel.setBorder(JBUI.Borders.empty(topLabelOffset, 0, bottomLabelOffset, 0));
      myCourseNameField.setBorder(createFieldBorder());

      if (EduSettings.getInstance().getUser() == null) {
        myInstructorLabel.setBorder(JBUI.Borders.emptyTop(topLabelOffset));
        myInstructorField.setBorder(createFieldBorder());
        myInstructorField.setEditable(EduSettings.getInstance().getUser() == null);
      }
      else {
        myInstructorLabel.setVisible(false);
        myInstructorField.setVisible(false);
      }

      myLanguageLabel.setBorder(JBUI.Borders.emptyTop(topLabelOffset));
      myHumanLanguageField.setBorder(createFieldBorder());

      myDescriptionLabel.setBorder(JBUI.Borders.emptyTop(topLabelOffset));
      myDescriptionArea.setBorder(createFieldBorder());
      myDescriptionArea.setRows(8);

      myAdvancedSettings.setVisible(false);

      myLocationField = createLocationComponent();

      myErrorLabel.setVisible(false);
      myErrorLabel.setForeground(MessageType.ERROR.getTitleForeground());

      UIUtil.setBackgroundRecursively(ObjectUtils.chooseNotNull(this.getRootPane(), this), EDIT_VIEW_BACKGROUND);
      myCourseNameField.setBackground(UIUtil.getTextFieldBackground());
      myInstructorField.setBackground(UIUtil.getTextFieldBackground());
      myHumanLanguageField.setBackground(UIUtil.getTextFieldBackground());
      myDescriptionArea.setBackground(UIUtil.getTextFieldBackground());
    }

    @NotNull
    private Border createFieldBorder() {
      return new RoundedBorderWithPadding(ARC_SIZE, false, UIUtil.getTextFieldBackground(), UIUtil.getTextFieldBackground());
    }

    private void setupValidation() {
      DocumentAdapter adapter = new DocumentAdapter() {
        @Override
        protected void textChanged(DocumentEvent event) {
          doValidation();
        }
      };
      myCourseNameField.getDocument().addDocumentListener(adapter);
      myInstructorField.getDocument().addDocumentListener(adapter);
      myHumanLanguageField.getDocument().addDocumentListener(adapter);
      myDescriptionArea.getDocument().addDocumentListener(adapter);
    }

    private void doValidation() {
      final String message;
      if (StringUtil.isEmpty(myCourseNameField.getText())) {
        message = "Enter course name";
        myCourseNameField.setBorder(new RoundedBorderWithPadding(ARC_SIZE, true, MessageType.ERROR.getPopupBackground(), UIUtil.getTextFieldBackground()));
      }
      else if (myInstructorField.isVisible() && StringUtil.isEmpty(myInstructorField.getText())) {
        message = "Enter course instructor";
        myInstructorField.setBorder(new RoundedBorderWithPadding(ARC_SIZE, true, MessageType.ERROR.getPopupBackground(), UIUtil.getTextFieldBackground()));
      }
      else if (StringUtil.isEmpty(myHumanLanguageField.getText())) {
        message = "Enter course language";
        // TODO: add validation from liana's yaml commits
        myHumanLanguageField.setBorder(new RoundedBorderWithPadding(ARC_SIZE, true, MessageType.ERROR.getPopupBackground(), UIUtil.getTextFieldBackground()));
      }
      else if (StringUtil.isEmpty(myDescriptionArea.getText())) {
        message = "Enter course description";
        myDescriptionArea.setBorder(new RoundedBorderWithPadding(ARC_SIZE, true, MessageType.ERROR.getPopupBackground(), UIUtil.getTextFieldBackground()));
      }
      else {
        message = null;
      }

      if (message != null) {
        myErrorLabel.setVisible(true);
        myErrorLabel.setText(message);
      }
      else {
        myCourseNameField.setBorder(new RoundedBorderWithPadding(ARC_SIZE, false, JBColor.border(), UIUtil.getTextFieldBackground()));
        myInstructorField.setBorder(new RoundedBorderWithPadding(ARC_SIZE, false, JBColor.border(), UIUtil.getTextFieldBackground()));
        myHumanLanguageField.setBorder(new RoundedBorderWithPadding(ARC_SIZE, false, JBColor.border(), UIUtil.getTextFieldBackground()));
        myDescriptionArea.setBorder(new RoundedBorderWithPadding(ARC_SIZE, false, JBColor.border(), UIUtil.getTextFieldBackground()));

        myErrorLabel.setVisible(false);
      }

      for (ValidationListener listener : myValidationListeners) {
       listener.onInputDataValidated(message == null);
      }
    }

    public String getCourseName() {
      return myCourseNameField.getText();
    }

    public String getAuthor() {
      if (!myInstructorField.isVisible()) {
        return authorsAsString(myCourse);
      }
      return myInstructorField.getText();
    }

    public String getLanguage() {
      return myHumanLanguageField.getText();
    }

    public String getDescription() {
      return myDescriptionArea.getText();
    }

    public void applyChanges(@NotNull Course course) {
      course.setName(myCourseNameField.getText());
      course.setLanguage(myHumanLanguageField.getText());
      course.setDescription(myDescriptionArea.getText());
      if (myInstructorField.isVisible()) {
        String[] authors = Arrays.stream(myInstructorField.getText().split(",")).map(name -> name.trim()).toArray(String[]::new);
        course.setAuthorsAsString(authors);
      }
    }
  }

  private class RenderedViewPanel extends JPanel {
    private JBLabel myCourseNameLabel;
    private TagsPanel myTagsPanel;
    private JEditorPane myInstructorPane;
    private JBScrollPane myInfoScroll;
    private JPanel myInfoPanel;
    private JEditorPane myDescriptionPane;
    private EduCourseBuilder.LanguageSettings<?> myLanguageSettings;
    private String myRawLanguage;
    private String myRawDescription;
    private String myAuthorsString;

    boolean isIndependentPanel;
    boolean isLocationFieldNeeded;

    public RenderedViewPanel(boolean isIndependentPanel, boolean isLocationFieldNeeded) {
      this.isIndependentPanel = isIndependentPanel;
      this.isLocationFieldNeeded = isLocationFieldNeeded;

      setLayout(new VerticalFlowLayout());

      myCourseNameLabel = new JBLabel();

      myTagsPanel = new TagsPanel();
      myInstructorPane = new JEditorPane();

      myDescriptionPane = new JEditorPane();
      myInfoScroll = new JBScrollPane(myDescriptionPane);
      myInfoPanel = new ScrollablePanel(new BorderLayout());
      myInfoPanel.add(myInfoScroll, BorderLayout.CENTER);

      add(myCourseNameLabel);
      add(myTagsPanel);
      add(myInstructorPane);
      add(myInfoPanel);

      initUI(isIndependentPanel, isLocationFieldNeeded);
    }

    public void setAllFields(@NotNull String courseName, @Nullable List<Tag> tags, @Nullable String authorsString, @NotNull String language, @NotNull String description) {
      initUI(isIndependentPanel, isLocationFieldNeeded);

      myCourseNameLabel.setText(courseName);

      if (tags != null) {
        if (myRawLanguage != null && !language.toLowerCase().equals(myRawLanguage.toLowerCase())) {
          ArrayList<Tag> newTags = new ArrayList<>();
          for (Tag tag : tags) {
            if (tag instanceof HumanLanguageTag) {
              newTags.add(new HumanLanguageTag(language));
            }
            else {
              newTags.add(tag);
            }
          }
          myTagsPanel.updateTags(newTags);
        }
        else {
          myTagsPanel.updateTags(tags);
        }
      }

      myAuthorsString = authorsString;
      if (myAuthorsString == null) {
        myInstructorPane.setPreferredSize(JBUI.size(0, 0));
      }
      else {
        myInstructorPane.setPreferredSize(null);
        myInstructorPane.setText(htmlInstructorText(authorsString));
      }
      myRawLanguage = language;
      myRawDescription = description;
      myDescriptionPane.setText(htmlDescription(myRawDescription));
    }

    private void initUI(boolean isIndependentPanel, boolean isLocationFieldNeeded) {
      int leftMargin;
      if (isIndependentPanel) {
        leftMargin = LARGE_HORIZONTAL_MARGIN;
      } else {
        leftMargin = HORIZONTAL_MARGIN;
      }

      myCourseNameLabel.setBorder(JBUI.Borders.empty(10, leftMargin, 5, HORIZONTAL_MARGIN));
      Font labelFont = UIUtil.getLabelFont();
      myCourseNameLabel.setFont(new Font(labelFont.getName(), Font.BOLD, JBUI.scaleFontSize(18.0f)));

      myTagsPanel.setBorder(JBUI.Borders.empty(5, leftMargin, 0, HORIZONTAL_MARGIN));

      setTextAreaAttributes(myInstructorPane, leftMargin, 15);
      setTextAreaAttributes(myDescriptionPane, leftMargin, 5);

      myInfoScroll.setBorder(null);

      myAdvancedSettings.setVisible(false);

      if (isLocationFieldNeeded) {
        myLocationField = createLocationComponent();
      }

      myDescriptionPane.addHyperlinkListener(new BrowserHyperlinkListener());

      UIUtil.setBackgroundRecursively(ObjectUtils.chooseNotNull(this.getRootPane(), this), UIUtil.getPanelBackground());
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
      setAllFields(course.getName(), course.getTags(), authorsAsString(course),
                   course.getHumanLanguage(),
                   course.getDescription() != null ? course.getDescription() : "");
    }

    public Object getSettings() {
      return myLanguageSettings.getSettings();
    }

    public String getCourseName() {
      return myCourseNameLabel.getText();
    }

    public String getLanguage() {
      return myRawLanguage;
    }

    public String getDescription() {
      return myRawDescription;
    }

    public void applyChanges(@NotNull Course course) {
      course.setName(myCourseNameLabel.getName());
      if (EduSettings.getInstance().getUser() == null) {
        course.setAuthorsAsString(Arrays.stream(myAuthorsString.split(",")).map(name -> name.trim()).toArray(String[]::new));
      }
      course.setDescription(myRawDescription);
      course.setLanguage(myRawLanguage);
    }

    public String getAuthorsString() {
      return myAuthorsString;
    }
  }

  private class TagsPanel extends JPanel {


    public TagsPanel() {
      setLayout(new HorizontalLayout(JBUI.scale(5)));
    }

    private void addTags(List<Tag> tags) {
      for (Tag tag : tags) {
        add(createTagLabel(tag));
      }
    }

    private void updateTags(List<Tag> tags) {
      removeAll();
      addTags(tags);
      revalidate();
      repaint();
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
  }

  public interface ValidationListener {
    void onInputDataValidated(boolean isInputDataComplete);
  }

  public void bindCourse(@NotNull Course course) {
    myCourse = course;
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

  private static String authorsAsString(@NotNull Course course) {
    return StringUtil.join(course.getAuthorFullNames(), ", ");
  }

  @Nullable
  private static String htmlInstructorText(@NotNull String authorsString) {
    StringBuilder builder = new StringBuilder();
    int authorsNumber = authorsString.split(",").length;
    if (authorsNumber == 0) return null;
    builder.append("<b>Instructor");
    if (authorsNumber > 1) {
      builder.append("s");
    }
    builder.append("</b>: ");
    builder.append(authorsString);
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

  public void bindSearchField(@NotNull FilterComponent searchField) {
    mySearchField = searchField;
  }
}
