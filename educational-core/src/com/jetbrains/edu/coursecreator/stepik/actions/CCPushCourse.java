package com.jetbrains.edu.coursecreator.stepik.actions;

import com.intellij.ide.IdeView;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.application.Experiments;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task.Modal;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.jetbrains.edu.coursecreator.CCUtils;
import com.jetbrains.edu.coursecreator.stepik.StepikCourseUploader;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.Lesson;
import com.jetbrains.edu.learning.courseFormat.Section;
import com.jetbrains.edu.learning.courseFormat.ext.CourseExt;
import com.jetbrains.edu.learning.statistics.EduUsagesCollector;
import com.jetbrains.edu.learning.stepik.StepikUpdateDateExt;
import com.jetbrains.edu.learning.stepik.StepikUtils;
import com.jetbrains.edu.learning.stepik.courseFormat.StepikChangeStatus;
import com.jetbrains.edu.learning.stepik.courseFormat.StepikCourse;
import com.jetbrains.edu.learning.stepik.courseFormat.ext.StepikCourseExt;
import com.jetbrains.edu.learning.stepik.courseFormat.ext.StepikLessonExt;
import com.jetbrains.edu.learning.stepik.courseFormat.ext.StepikSectionExt;
import com.jetbrains.edu.learning.stepik.courseFormat.remoteInfo.StepikCourseRemoteInfo;
import com.jetbrains.edu.learning.stepik.courseFormat.remoteInfo.StepikRemoteInfo;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static com.jetbrains.edu.coursecreator.stepik.CCStepikConnector.*;

@SuppressWarnings("ComponentNotRegistered") // educational-core.xml
public class CCPushCourse extends DumbAwareAction {
  private static Logger LOG = Logger.getInstance(CCPushCourse.class);

  public CCPushCourse() {
    super("&Upload Course to Stepik", "Upload Course to Stepik", null);
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    Presentation presentation = e.getPresentation();
    Project project = e.getProject();
    presentation.setEnabledAndVisible(project != null && CCUtils.isCourseCreator(project));
    if (project != null) {
      final Course course = StudyTaskManager.getInstance(project).getCourse();
      if (course instanceof StepikCourse) {
        presentation.setText("Update Course on Stepik");
      }
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
    if (course == null) {
      return;
    }
    if (doPush(project, course)) return;
    EduUsagesCollector.courseUploaded();
  }

  public static boolean doPush(Project project, Course course) {
    if (course instanceof StepikCourse) {
      final StepikCourse stepikCourse = (StepikCourse)course;
      askToWrapTopLevelLessons(project, stepikCourse);
      ProgressManager.getInstance().run(new Modal(project, "Updating Course", true) {
        @Override
        public void run(@NotNull ProgressIndicator indicator) {
          if (getCourseInfo(String.valueOf(stepikCourse.getStepikRemoteInfo().getId())) == null) {
            String message = "Cannot find course on Stepik. <br> <a href=\"upload\">Upload to Stepik as New Course</a>";
            Notification notification = new Notification("update.course", "Failed ot update", message, NotificationType.ERROR,
                                                         createPostCourseNotificationListener(project, stepikCourse));
            notification.notify(project);
            return;
          }
          indicator.setIndeterminate(false);

          if (Experiments.isFeatureEnabled(StepikCourseUploader.FEATURE_ID)) {
            new StepikCourseUploader(project, stepikCourse).updateCourse();
          }
          else {
            pushInOldWay(indicator, project, stepikCourse);
          }
        }
      });
    }
    else {
      if (CourseExt.getHasSections(course) && CourseExt.getHasTopLevelLessons(course)) {
        int result = Messages
          .showYesNoDialog(project, "Since you have sections, we have to wrap top-level lessons into section before upload",
                           "Wrap Lessons Into Sections", "Wrap and Post", "Cancel", null);
        if (result == Messages.YES) {
          wrapUnpushedLessonsIntoSections(project, course);
        }
        else {
          return true;
        }
      }
      postCourseWithProgress(project, course);
    }
    return false;
  }

  private static void pushInOldWay(@NotNull ProgressIndicator indicator, Project project, StepikCourse course) {
    if (updateCourseInfo(project, course)) {
      updateCourseContent(indicator, course, project);
      StepikUtils.setStatusRecursively(course, StepikChangeStatus.UP_TO_DATE);
      try {
        updateAdditionalMaterials(project, StepikCourseExt.getId(course));
      }
      catch (IOException e1) {
        LOG.warn(e1);
      }

      StepikUpdateDateExt.setUpdated(course);
      showNotification(project, "Course is updated", openOnStepikAction("/course/" + StepikCourseExt.getId(course)));
    }
  }

  private static void askToWrapTopLevelLessons(Project project, StepikCourse course) {
    if (CourseExt.getHasSections(course) && CourseExt.getHasTopLevelLessons(course)) {
      boolean hasUnpushedLessons = course.getLessons().stream().anyMatch(lesson -> !StepikLessonExt.isStepikLesson(lesson));
      if (hasUnpushedLessons) {
        int result = Messages
          .showYesNoDialog(project, "Top-level lessons will be wrapped with sections as it's not allowed to have both top-level lessons and sections",
                           "Wrap Lessons Into Sections", "Wrap and Post", "Cancel", null);
        if (result == Messages.YES) {
          wrapUnpushedLessonsIntoSections(project, course);
        }
      }
    }
  }

  private static void updateCourseContent(@NotNull ProgressIndicator indicator, StepikCourse course, Project project) {
    final StepikCourseRemoteInfo stepikRemoteInfo = course.getStepikRemoteInfo();

    final List<Integer> sectionIds = stepikRemoteInfo.getSectionIds();
    if (!sectionIds.isEmpty() && course.getLessons().isEmpty()) {
      deleteSection(sectionIds.get(0));
      stepikRemoteInfo.setSectionIds(Collections.emptyList());
    }

    int position = 1 + (CourseExt.getHasTopLevelLessons(course) ? 1 : 0);
    for (Section section : course.getSections()) {
      StepikSectionExt.setPosition(section, position++);
      if (section.getRemoteInfo() instanceof StepikRemoteInfo) {
        updateSection(project, section);
      }
      else {
        postSection(project, section, indicator);
        updateAdditionalSection(project);
      }
    }

    for (Lesson lesson : course.getLessons()) {
      Integer sectionId = stepikRemoteInfo.getSectionIds().get(0);
      if (StepikLessonExt.isStepikLesson(lesson)) {
        updateLesson(project, lesson, false, sectionId);
      }
      else {
        postLesson(project, lesson, lesson.getIndex(), sectionId);
      }
    }
  }
}