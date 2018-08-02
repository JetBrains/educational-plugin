package com.jetbrains.edu.python.learning.checkio.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOCourse;
import com.jetbrains.edu.learning.courseFormat.Course;

public class PyCheckiOUpdateCourseAction extends DumbAwareAction {
  private static Logger LOG = Logger.getInstance(PyCheckiOUpdateCourseAction.class);

  @Override
  public void actionPerformed(AnActionEvent event) {
    final Project project = event.getProject();
    if (project != null) {
      final Course course = StudyTaskManager.getInstance(project).getCourse();
      if (course instanceof CheckiOCourse) {
        final CheckiOCourse checkioCourse = (CheckiOCourse) course;

      }
    }
  }
}
