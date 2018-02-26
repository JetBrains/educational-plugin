package com.jetbrains.edu.python.coursecreator;

import com.intellij.openapi.project.Project;
import com.jetbrains.edu.coursecreator.actions.CCChangeCourseInfo;
import com.jetbrains.edu.coursecreator.ui.CCEditCourseInfoDialog;
import com.jetbrains.edu.learning.courseFormat.Course;
import org.jetbrains.annotations.NotNull;

public class PyCCChangeCourseInfo extends CCChangeCourseInfo {

  public PyCCChangeCourseInfo() {
    super();
  }

  @Override
  @NotNull
  public CCEditCourseInfoDialog createDialog(@NotNull Project project, @NotNull Course course, @NotNull String title) {
    return new PyCCEditCourseInfoDialog(project, course, title);
  }

}
