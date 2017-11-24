package com.jetbrains.edu.coursecreator.actions.stepik;

import com.intellij.ide.IdeView;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.text.StringUtil;
import com.jetbrains.edu.coursecreator.actions.CCFromCourseArchive;
import com.jetbrains.edu.coursecreator.stepik.CCStepikConnector;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.RemoteCourse;
import com.jetbrains.edu.learning.stepik.StepikConnector;
import org.jetbrains.annotations.NotNull;

public class CCGetCourseFromStepik extends DumbAwareAction {

  public CCGetCourseFromStepik() {
    super("Get Course From Stepik", "Get Course From Stepik", null);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    final IdeView view = e.getData(LangDataKeys.IDE_VIEW);
    final Project project = e.getData(CommonDataKeys.PROJECT);
    if (view == null || project == null) {
      return;
    }
    final String courseId = Messages.showInputDialog("Please, enter course id", "Get Course From Stepik", null);
    if (StringUtil.isNotEmpty(courseId)) {
      ProgressManager.getInstance().run(new Task.Modal(project, "Creating Course", true) {
        @Override
        public void run(@NotNull final ProgressIndicator indicator) {
          createCourse(project, courseId);
        }
      });
    }
  }

  private static void createCourse(Project project, String courseId) {
    final RemoteCourse info = CCStepikConnector.getCourseInfo(courseId);
    if (info == null) return;

    final Course course = StepikConnector.getCourse(project, info);
    if (course == null) return;

    CCFromCourseArchive.generateFromStudentCourse(project, course);
  }
}