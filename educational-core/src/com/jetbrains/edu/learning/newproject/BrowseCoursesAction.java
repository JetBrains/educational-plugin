package com.jetbrains.edu.learning.newproject;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.ui.DialogBuilder;
import com.intellij.openapi.ui.DialogWrapper;
import com.jetbrains.edu.learning.EduConfigurator;
import com.jetbrains.edu.learning.EduConfiguratorManager;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.newproject.ui.CoursesPanel;
import icons.EducationalCoreIcons;

import java.util.List;

public class BrowseCoursesAction extends DumbAwareAction {
  public BrowseCoursesAction() {
    super("Browse Courses", "Browse list of available courses", EducationalCoreIcons.CourseAction);
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    List<Course> courses = EduUtils.getCoursesUnderProgress();
    if (courses == null) return;

    CoursesPanel panel = new CoursesPanel(courses);
    DialogBuilder dialogBuilder = new DialogBuilder().title("Select Course").centerPanel(panel);
    dialogBuilder.addOkAction().setText("Join");
    panel.addCourseValidationListener(dialogBuilder::setOkActionEnabled);
    dialogBuilder.setOkOperation(() -> {
      dialogBuilder.getDialogWrapper().close(DialogWrapper.OK_EXIT_CODE);
      Course course = panel.getSelectedCourse();
      Object projectSettings = panel.getProjectSettings();
      String location = panel.getLocationString();
      EduConfigurator configurator = EduConfiguratorManager.forLanguage(course.getLanguageById());
      if (configurator != null) {
        CourseProjectGenerator projectGenerator = configurator
                .getCourseBuilder()
                .getCourseProjectGenerator(course);
        if (projectGenerator != null) {
          projectGenerator.doCreateCourseProject(location, projectSettings);
        }
      }
    });
    dialogBuilder.show();
  }
}
