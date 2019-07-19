package com.jetbrains.edu.coursecreator.actions.stepik;

import com.intellij.ide.IdeView;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
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
import com.jetbrains.edu.coursecreator.stepik.StepikCourseUploader;
import com.jetbrains.edu.coursecreator.yaml.YamlFormatSynchronizer;
import com.jetbrains.edu.learning.EduNames;
import com.jetbrains.edu.learning.EduVersions;
import com.jetbrains.edu.learning.PluginUtils;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.EduCourse;
import com.jetbrains.edu.learning.courseFormat.ext.CourseExt;
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector;
import com.jetbrains.edu.learning.stepik.api.StepikConnector;
import org.jetbrains.annotations.NotNull;

import javax.swing.event.HyperlinkEvent;

import static com.jetbrains.edu.coursecreator.CCUtils.askToWrapTopLevelLessons;
import static com.jetbrains.edu.coursecreator.stepik.CCStepikConnector.*;
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

    if (!checkIfAuthorized(project, ((EduCourse)course).isRemote() ? "update course" : "post course")) {
      return;
    }

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

  public static void doPush(Project project, @NotNull EduCourse course) {
    if (course.isRemote()) {
      EduCourse courseInfo = StepikConnector.getInstance().getCourseInfo(course.getId(), null, true);
      if (courseInfo == null) {
        String message = "Cannot find course on Stepik. <br> <a href=\"upload\">Upload to Stepik as New Course</a>";
        Notification notification = new Notification("update.course", "Failed to update", message, NotificationType.ERROR,
                                                     createPostCourseNotificationListener(project, course));
        notification.notify(project);
        return;
      }
      if (courseInfo.getFormatVersion() < EduVersions.JSON_FORMAT_VERSION) {
        String message = "Updating this course will make it available since " +
                         PluginUtils.pluginVersion(EduNames.PLUGIN_ID) + " plugin version. <br> <a href=\"update\">Update course</a>";
        Notification notification = new Notification("update.course", "Format Version Mismatch", message,
                                                     NotificationType.WARNING,
                                                     createUpdateCourseNotificationListener(project, course));
        notification.notify(project);
        return;
      }
      updateCourse(project, course);
    }
    else {
      postCourseWithProgress(project, course);
      EduCounterUsageCollector.uploadCourse();
    }
  }

  @NotNull
  private static NotificationListener createUpdateCourseNotificationListener(Project project,
                                                                             @NotNull EduCourse course) {
    return new NotificationListener() {
      @Override
      public void hyperlinkUpdate(@NotNull Notification notification,
                                  @NotNull HyperlinkEvent event) {
        notification.expire();
        updateCourse(project, course);
      }
    };
  }

  private static void updateCourse(Project project, @NotNull EduCourse course) {
    ProgressManager.getInstance().run(new Modal(project, "Updating Course", true) {
      @Override
      public void run(@NotNull ProgressIndicator indicator) {
        indicator.setIndeterminate(false);
        new StepikCourseUploader(project, course).updateCourse();
        EduCounterUsageCollector.updateCourse();
      }
    });
  }
}