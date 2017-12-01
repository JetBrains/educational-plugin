package com.jetbrains.edu.learning.newproject.ui;

import com.google.common.collect.Lists;
import com.intellij.icons.AllIcons;
import com.intellij.lang.Language;
import com.intellij.openapi.actionSystem.ActionToolbarPosition;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.OnePixelDivider;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.*;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBList;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.jetbrains.edu.learning.EduLanguageDecorator;
import com.jetbrains.edu.learning.EduSettings;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.RemoteCourse;
import com.jetbrains.edu.learning.courseFormat.Tag;
import com.jetbrains.edu.learning.statistics.EduUsagesCollector;
import com.jetbrains.edu.learning.stepic.StepicConnector;
import com.jetbrains.edu.learning.stepic.StepicUser;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

public class CoursesPanel extends JPanel {
  private static final JBColor LIST_COLOR = new JBColor(Gray.xFF, Gray.x39);
  private static final Logger LOG = Logger.getInstance(CoursesPanel.class);
  private static final String NO_COURSES = "No courses found";

  private JPanel myMainPanel;
  private JPanel myCourseListPanel;
  private FilterComponent mySearchField;
  private JBLabel myErrorLabel;
  private JSplitPane mySplitPane;
  private JPanel mySplitPaneRoot;
  private JBList<Course> myCoursesList;
  private CoursePanel myCoursePanel;
  private List<Course> myCourses;
  private List<CourseValidationListener> myListeners = new ArrayList<>();
  private final List<Integer> myFeaturedCourses = EduUtils.getFeaturedCourses();

  public CoursesPanel(@NotNull List<Course> courses) {
    myCourses = courses;
    setLayout(new BorderLayout());
    add(myMainPanel, BorderLayout.CENTER);
    initUI();
  }

  private void initUI() {
    GuiUtils.replaceJSplitPaneWithIDEASplitter(mySplitPaneRoot, true);
    mySplitPane.setDividerLocation(0.5);
    mySplitPane.setResizeWeight(0.5);
    myCoursesList = new JBList<>();
    myCoursesList.setEmptyText(NO_COURSES);
    updateModel(myCourses, null);
    myErrorLabel.setVisible(false);
    myErrorLabel.setBorder(JBUI.Borders.empty(20, 10, 0, 0));

    ColoredListCellRenderer<Course> renderer = getCourseRenderer();
    myCoursesList.setCellRenderer(renderer);
    myCoursesList.addListSelectionListener(e -> processSelectionChanged());
    DefaultActionGroup group = new DefaultActionGroup(new AnAction("Import Course", "import local course", AllIcons.ToolbarDecorator.Import) {
      @Override
      public void actionPerformed(AnActionEvent e) {
        final FileChooserDescriptor fileChooser = new FileChooserDescriptor(true, false, false, true, false, false) {
          @Override
          public boolean isFileVisible(VirtualFile file, boolean showHiddenFiles) {
            return file.isDirectory() || EduUtils.isZip(file.getName());
          }

          @Override
          public boolean isFileSelectable(VirtualFile file) {
            return EduUtils.isZip(file.getName());
          }

        };
        FileChooser.chooseFile(fileChooser, null, VfsUtil.getUserHomeDir(),
                               file -> {
                                 String fileName = file.getPath();
                                 Course course = EduUtils.getLocalCourse(fileName);
                                 if (course != null) {
                                   EduUsagesCollector.courseArchiveImported();
                                   myCourses.add(course);
                                   updateModel(myCourses, course.getName());
                                 } else {
                                   Messages.showErrorDialog("Selected archive doesn't contain a valid course", "Failed to Add Local Course");
                                 }
                               });
      }
    });

    ToolbarDecorator toolbarDecorator = ToolbarDecorator.createDecorator(myCoursesList).
      disableAddAction().disableRemoveAction().disableUpDownActions().setActionGroup(group).setToolbarPosition(ActionToolbarPosition.BOTTOM);
    JPanel toolbarDecoratorPanel = toolbarDecorator.createPanel();
    toolbarDecoratorPanel.setBorder(null);
    myCoursesList.setBorder(null);
    myCourseListPanel.add(toolbarDecoratorPanel, BorderLayout.CENTER);
    myCourseListPanel.setBorder(JBUI.Borders.customLine(OnePixelDivider.BACKGROUND, 1, 1, 1, 1));
    myCoursesList.setBackground(LIST_COLOR);
    myErrorLabel.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        if (!isLoggedIn() && myErrorLabel.isVisible()) {
          ApplicationManager.getApplication().getMessageBus().connect().subscribe(EduSettings.SETTINGS_CHANGED, () -> {
            StepicUser user = EduSettings.getInstance().getUser();
            if (user != null) {
              ApplicationManager.getApplication().invokeLater(() -> {
                Course selectedCourse = myCoursesList.getSelectedValue();
                List<Course> courses = EduUtils.getCoursesUnderProgress();
                myCourses = courses != null ? courses : Lists.newArrayList();
                updateModel(myCourses, selectedCourse.getName());
                myErrorLabel.setVisible(false);
                notifyListeners(true);
              }, ModalityState.any());
            }
          });
          StepicConnector.doAuthorize(() -> EduUtils.showOAuthDialog());
        }
      }

      @Override
      public void mouseEntered(MouseEvent e) {
        if (!isLoggedIn() && myErrorLabel.isVisible()) {
          e.getComponent().setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
      }

      @Override
      public void mouseExited(MouseEvent e) {
        if (!isLoggedIn() && myErrorLabel.isVisible()) {
          e.getComponent().setCursor(Cursor.getDefaultCursor());
        }
      }
    });

    processSelectionChanged();
  }

  @NotNull
  private ColoredListCellRenderer<Course> getCourseRenderer() {
    return new ColoredListCellRenderer<Course>() {
        @Override
        protected void customizeCellRenderer(@NotNull JList<? extends Course> jList, Course course, int i, boolean b, boolean b1) {
          boolean isPrivate = course instanceof RemoteCourse && !((RemoteCourse)course).isPublic();
          Icon logo = getLogo(course);
          setBorder(JBUI.Borders.empty(5, 0));

          if ((course instanceof RemoteCourse && myFeaturedCourses.contains(((RemoteCourse) course).getId())) ||
              myFeaturedCourses.isEmpty()) {
            append(course.getName());
            setIcon(logo);
            setToolTipText(null);
          }
          else if (isPrivate) {
            append(course.getName());
            setIcon(getPrivateCourseIcon(logo));
            setToolTipText("Course is private");
          } else {
            append(course.getName(), SimpleTextAttributes.GRAYED_ATTRIBUTES);
            setIcon(getNotApprovedCourseIcon(logo));
            setToolTipText("Course has not been approved by JetBrains yet");
          }
        }

        @NotNull
        public LayeredIcon getPrivateCourseIcon(@Nullable Icon languageLogo) {
          LayeredIcon icon = new LayeredIcon(2);
          icon.setIcon(languageLogo, 0, 0, 0);
          icon.setIcon(AllIcons.Ide.Readonly, 1, JBUI.scale(7), JBUI.scale(7));
          return icon;
        }

        @Nullable
        public Icon getNotApprovedCourseIcon(@Nullable Icon languageLogo) {
          return languageLogo != null ? IconLoader.getTransparentIcon(languageLogo) : null;
        }
      };
  }

  private void processSelectionChanged() {
    Course selectedCourse = myCoursesList.getSelectedValue();
    notifyListeners(canStartCourse(selectedCourse));
    if (selectedCourse != null) {
      updateCourseInfoPanel(selectedCourse);
    }
  }

  private void updateCourseInfoPanel(Course selectedCourse) {
    myCoursePanel.bindCourse(selectedCourse);
    if (!isLoggedIn()) {
      myErrorLabel.setVisible(true);
      myErrorLabel.setText(
        UIUtil.toHtml("<u><b>Log in</b></u> to Stepik " + (selectedCourse.isAdaptive() ? "to start adaptive course" : "to see more courses")));
      myErrorLabel.setForeground((selectedCourse.isAdaptive() ? MessageType.ERROR : MessageType.WARNING).getTitleForeground());
    }
  }

  private static boolean isLoggedIn() {
    return EduSettings.getInstance().getUser() != null;
  }

  private int getWeight(@NotNull Course course) {
    final int id = course instanceof RemoteCourse ? ((RemoteCourse) course).getId() : 0;
    if (course instanceof RemoteCourse && !((RemoteCourse) course).isPublic()) {
      return 1;
    }
    if (myFeaturedCourses.contains(id)) {
      return 2;
    }
    return 3;
  }

  private List<Course> sortCourses(List<Course> courses) {
    final Map<Integer, List<Course>> groupedCourses = courses.stream()
                                                          .sorted(Comparator.comparing(course -> course.getName()))
                                                          .collect(groupingBy((course) -> getWeight(course)));
    return groupedCourses.values().stream().flatMap(Collection::stream).collect(toList());
  }

  private void updateModel(List<Course> courses, @Nullable String courseToSelect) {
    DefaultListModel<Course> listModel = new DefaultListModel<>();
    courses = sortCourses(courses);
    for (Course course : courses) {
      listModel.addElement(course);
    }
    myCoursesList.setModel(listModel);
    if (myCoursesList.getItemsCount() > 0) {
      myCoursesList.setSelectedIndex(0);
    } else {
      myCoursePanel.hideContent();
    }
    if (courseToSelect == null) {
      return;
    }
    myCourses.stream()
        .filter(course -> course.getName().equals(courseToSelect))
        .findFirst()
        .ifPresent(newCourseToSelect -> myCoursesList.setSelectedValue(newCourseToSelect, true));
  }

  public Course getSelectedCourse() {
    return myCoursesList.getSelectedValue();
  }

  private void createUIComponents() {
    myCoursePanel = new CoursePanel(false, true);
    mySearchField = new FilterComponent("Edu.NewCourse", 5, true) {
      @Override
      public void filter() {
        Course selectedCourse = myCoursesList.getSelectedValue();
        String filter = getFilter();
        List<Course> filtered = new ArrayList<>();
        for (Course course : myCourses) {
          if (accept(filter, course)) {
            filtered.add(course);
          }
        }
        updateModel(filtered, selectedCourse != null ? selectedCourse.getName() : null);
      }
    };
    myCoursePanel.bindSearchField(mySearchField);
    UIUtil.setBackgroundRecursively(mySearchField, UIUtil.getTextFieldBackground());
  }

  public boolean accept(@NonNls String filter, Course course) {
    if (filter.isEmpty()) {
      return true;
    }

    final Set<String> filterParts = getFilterParts(filter);
    final String courseName = course.getName().toLowerCase(Locale.getDefault());

    for (String filterPart : filterParts) {
      if (courseName.contains(filterPart))
        return true;

      for (Tag tag : course.getTags()) {
        if (tag.accept(filterPart)) {
          return true;
        }
      }

      for (String authorName : course.getAuthorFullNames()) {
        if (authorName.toLowerCase(Locale.getDefault()).contains(filterPart)) {
          return true;
        }
      }
    }
    return false;
  }

  @Nullable
  private static Icon getLogo(@NotNull Course course) {
    Language language = course.getLanguageById();
    EduLanguageDecorator decorator = EduLanguageDecorator.INSTANCE.forLanguage(language);
    if (decorator == null) {
      LOG.info("language decorator is null, language: " + language.getDisplayName());
      return null;
    }
    return decorator.getLogo();
  }

  @NotNull
  public String getLocationString() {
    String locationString = myCoursePanel.getLocationString();
    // We use `myCoursePanel` with location field
    // so `myCoursePanel.getLocationString()` must return not null value
    assert locationString != null;
    return locationString;
  }

  @NotNull
  public Object getProjectSettings() {
    return myCoursePanel.getProjectSettings();
  }

  @Override
  public Dimension getPreferredSize() {
    return JBUI.size(600, 400);
  }

  public void addCourseValidationListener(CourseValidationListener listener) {
    myListeners.add(listener);
    listener.validationStatusChanged(canStartCourse(myCoursesList.getSelectedValue()));
  }

  private void notifyListeners(boolean canStartCourse) {
    for (CourseValidationListener listener : myListeners) {
      listener.validationStatusChanged(canStartCourse);
    }
  }

  private static boolean canStartCourse(Course selectedCourse) {
    if (selectedCourse == null) {
      return false;
    }

    if (isLoggedIn()) {
      return true;
    }

    return !selectedCourse.isAdaptive();
  }

  public interface CourseValidationListener {
    void validationStatusChanged(boolean canStartCourse);
  }

  public static Set<String> getFilterParts(String filter) {
    return new HashSet<>(Arrays.asList(filter.toLowerCase().split(" ")));
  }
}
