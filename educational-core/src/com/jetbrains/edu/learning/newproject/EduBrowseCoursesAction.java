package com.jetbrains.edu.learning.newproject;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.DialogBuilder;
import com.intellij.openapi.ui.DialogWrapper;
import com.jetbrains.edu.learning.EduPluginConfigurator;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.newproject.ui.EduCoursesPanel;
import icons.EducationalCoreIcons;

public class EduBrowseCoursesAction extends AnAction {
  public EduBrowseCoursesAction() {
    super("Browse Courses", "Browse list of available courses", EducationalCoreIcons.Course);
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    EduCoursesPanel panel = new EduCoursesPanel();
    DialogBuilder dialogBuilder = new DialogBuilder().title("Select Course").centerPanel(panel);
    dialogBuilder.addOkAction().setText("Join");
    panel.addCourseValidationListener(dialogBuilder::setOkActionEnabled);
    dialogBuilder.setOkOperation(() -> {
      dialogBuilder.getDialogWrapper().close(DialogWrapper.OK_EXIT_CODE);
      Course course = panel.getSelectedCourse();
      String location = panel.getLocationString();
      EduPluginConfigurator pluginConfigurator = EduPluginConfigurator.INSTANCE.forLanguage(course.getLanguageById());
      if (pluginConfigurator != null) {
        EduCourseProjectGenerator projectGenerator = pluginConfigurator.getEduCourseProjectGenerator();
        if (projectGenerator != null) {
          projectGenerator.createProject(course, location);
        }
      }
    });
    dialogBuilder.show();
  }
}
