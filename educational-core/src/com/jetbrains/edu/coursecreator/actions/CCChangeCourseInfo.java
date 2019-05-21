package com.jetbrains.edu.coursecreator.actions;

import com.intellij.ide.IdeView;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogBuilder;
import com.intellij.profile.codeInspection.ProjectInspectionProfileManager;
import com.intellij.psi.PsiDirectory;
import com.jetbrains.edu.coursecreator.CCUtils;
import com.jetbrains.edu.coursecreator.stepik.StepikCourseChangeHandler;
import com.jetbrains.edu.coursecreator.ui.CCCourseInfoPanel;
import com.jetbrains.edu.coursecreator.yaml.YamlFormatSynchronizer;
import com.jetbrains.edu.learning.LanguageSettings;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.configuration.EduConfigurator;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.ext.CourseExt;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.List;

import static com.jetbrains.edu.learning.EduUtils.addMnemonic;

public class CCChangeCourseInfo extends DumbAwareAction {
  private static final String ACTION_TEXT = "Edit Course Information";

  public CCChangeCourseInfo() {
    super(addMnemonic(ACTION_TEXT), ACTION_TEXT, null);
  }

  @Override
  public void update(@NotNull AnActionEvent event) {
    final Project project = event.getProject();
    final Presentation presentation = event.getPresentation();
    if (project == null) {
      return;
    }
    presentation.setEnabledAndVisible(false);
    if (!CCUtils.isCourseCreator(project)) {
      return;
    }
    final IdeView view = event.getData(LangDataKeys.IDE_VIEW);
    if (view == null) {
      return;
    }
    final PsiDirectory[] directories = view.getDirectories();
    if (directories.length == 0) {
      return;
    }
    presentation.setEnabledAndVisible(true);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    if (project == null) {
      return;
    }
    Course course = StudyTaskManager.getInstance(project).getCourse();
    if (course == null) {
      return;
    }

    CCCourseInfoPanel panel =
      new CCCourseInfoPanel(course.getName(), Course.getAuthorsString(course.getAuthors()), course.getDescription());
    setupLanguageLevels(course, panel);
    DialogBuilder builder = createChangeInfoDialog(project, panel);
    panel.setValidationListener(builder::setOkActionEnabled);
    if (builder.showAndGet()) {
      course.setAuthorsAsString(panel.getAuthors());
      course.setName(panel.getName());
      course.setDescription(panel.getDescription());
      setVersion(course, panel);
      ProjectView.getInstance(project).refresh();
      ProjectInspectionProfileManager.getInstance(project).fireProfileChanged();
      StepikCourseChangeHandler.infoChanged(course);
      YamlFormatSynchronizer.saveItem(course);
    }
  }

  private static void setVersion(@NotNull final Course course, @NotNull final CCCourseInfoPanel panel) {
    String version = panel.getLanguageVersion();
    if (version == null) {
      return;
    }
    course.setLanguage(course.getLanguageID() + " " + version);
  }

  private static void setupLanguageLevels(@NotNull final Course course, @NotNull final CCCourseInfoPanel panel) {
    EduConfigurator<?> configurator = CourseExt.getConfigurator(course);
    if (configurator == null) {
      return;
    }
    LanguageSettings<?> languageSettings = configurator.getCourseBuilder().getLanguageSettings();
    List<String> languageVersions = languageSettings.getLanguageVersions();
    if (languageVersions.size() < 2) {
      return;
    }
    JLabel languageLevelLabel = panel.getLanguageLevelLabel();
    languageLevelLabel.setText(course.getLanguageById().getDisplayName() + ":");
    languageLevelLabel.setVisible(true);
    ComboBox<String> languageLevelCombobox = panel.getLanguageLevelCombobox();
    for (String version : languageVersions) {
      languageLevelCombobox.addItem(version);
    }
    languageLevelCombobox.setVisible(true);
    final String version = course.getLanguageVersion();
    if (version != null) {
      languageLevelCombobox.setSelectedItem(version);
    }
  }

  private static DialogBuilder createChangeInfoDialog(Project project, @NotNull CCCourseInfoPanel panel) {
    DialogBuilder builder = new DialogBuilder(project);

    builder.setTitle(ACTION_TEXT);
    JPanel changeInfoPanel = panel.getMainPanel();
    changeInfoPanel.setPreferredSize(new Dimension(450, 300));
    changeInfoPanel.setMinimumSize(new Dimension(450, 300));
    builder.setCenterPanel(changeInfoPanel);

    return builder;
  }
}
