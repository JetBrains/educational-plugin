package com.jetbrains.edu.coursecreator.actions.stepik;

import com.intellij.ide.IdeView;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task.Modal;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.jetbrains.edu.coursecreator.CCUtils;
import com.jetbrains.edu.coursecreator.stepik.CCStepikConnector;
import com.jetbrains.edu.coursecreator.stepik.StepikCourseUploader;
import com.jetbrains.edu.coursecreator.yaml.YamlFormatSynchronizer;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.EduCourse;
import com.jetbrains.edu.learning.courseFormat.ext.CourseExt;
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector;
import com.jetbrains.edu.learning.stepik.api.StepikConnector;
import org.jetbrains.annotations.NotNull;

import static com.jetbrains.edu.coursecreator.CCUtils.askToWrapTopLevelLessons;
import static com.jetbrains.edu.coursecreator.stepik.CCStepikConnector.createPostCourseNotificationListener;
import static com.jetbrains.edu.coursecreator.stepik.CCStepikConnector.postCourse;
import static com.jetbrains.edu.learning.EduUtils.addMnemonic;

@SuppressWarnings("ComponentNotRegistered") // educational-core.xml
public class CCPushCourse extends DumbAwareAction {

  private static final String ACTION_TEXT = "Upload Course to Stepik";

  public CCPushCourse() {
    super(addMnemonic(ACTION_TEXT), ACTION_TEXT, null);
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    Presentation presentation = e.getPresentation();
    Project project = e.getProject();
    presentation.setEnabledAndVisible(false);
    if (project == null || !CCUtils.isCourseCreator(project)) {
      return;
    }
    final Course course = StudyTaskManager.getInstance(project).getCourse();
    if (!(course instanceof EduCourse)) {
      return;
    }
    presentation.setEnabledAndVisible(true);
    if (((EduCourse)course).isRemote()) {
      presentation.setText("Update Course on Stepik");
    }
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    final IdeView view = e.getData(LangDataKeys.IDE_VIEW);
    final Project project = e.getData(CommonDataKeys.PROJECT);
    if (view == null || project == null) {
      return;
    }
    final Course course = StudyTaskManager.getInstance(project).getCourse();
    if (!(course instanceof EduCourse)) {
      return;
    }

    if (CourseExt.getHasSections(course) && CourseExt.getHasTopLevelLessons(course)) {
      if (!askToWrapTopLevelLessons(project, (EduCourse)course, "Wrap and Post")) {
        return;
      }
    }

    String title = ((EduCourse)course).isRemote() ? "Updating Course" : "Uploading Course";
    ProgressManager.getInstance().run(new Modal(project, title, true) {
      @Override
      public void run(@NotNull ProgressIndicator indicator) {
        indicator.setIndeterminate(false);
        doPush(project, (EduCourse)course);
        // we set course inside postCourse method that's why have to get it here again
        Course uploadedCourse = StudyTaskManager.getInstance(project).getCourse();
        if (uploadedCourse != null) {
          YamlFormatSynchronizer.saveRemoteInfo(uploadedCourse);
        }
        else {
          throw new IllegalStateException("Course is null while pushing course to stepik");
        }
      }
    });
  }

  public static boolean doPush(Project project, @NotNull EduCourse course) {
    if (!CCStepikConnector.checkIfAuthorized(project, course.isRemote() ? "update course" : "post course")) {
      return false;
    }

    if (course.isRemote()) {
      if (StepikConnector.getCourseInfo(course.getId(), null, true) == null) {
        String message = "Cannot find course on Stepik. <br> <a href=\"upload\">Upload to Stepik as New Course</a>";
        Notification notification = new Notification("update.course", "Failed to update", message, NotificationType.ERROR,
                                                     createPostCourseNotificationListener(project, course));
        notification.notify(project);
        return false;
      }

      new StepikCourseUploader(project, course).updateCourse();
      EduCounterUsageCollector.updateCourse();
    }
    else {
      postCourse(project, course);
      EduCounterUsageCollector.uploadCourse();
    }
    return false;
  }

}