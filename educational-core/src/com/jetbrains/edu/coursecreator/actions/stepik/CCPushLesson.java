package com.jetbrains.edu.coursecreator.actions.stepik;

import com.intellij.ide.IdeView;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiDirectory;
import com.jetbrains.edu.coursecreator.CCUtils;
import com.jetbrains.edu.coursecreator.stepik.CCStepikConnector;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.Lesson;
import com.jetbrains.edu.learning.courseFormat.RemoteCourse;
import com.jetbrains.edu.learning.courseFormat.Section;
import com.jetbrains.edu.learning.courseFormat.ext.CourseExt;
import com.jetbrains.edu.learning.stepik.StepikNames;
import com.twelvemonkeys.lang.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class CCPushLesson extends DumbAwareAction {
  public CCPushLesson() {
    super("Update Lesson on Stepik", "Update Lesson on Stepik", null);
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    e.getPresentation().setEnabledAndVisible(false);
    final IdeView view = e.getData(LangDataKeys.IDE_VIEW);
    final Project project = e.getData(CommonDataKeys.PROJECT);
    if (view == null || project == null) {
      return;
    }
    final Course course = StudyTaskManager.getInstance(project).getCourse();
    if (!(course instanceof RemoteCourse)) {
      return;
    }
    if (!course.getCourseMode().equals(CCUtils.COURSE_MODE)) return;
    final PsiDirectory[] directories = view.getDirectories();
    if (directories.length == 0 || directories.length > 1) {
      return;
    }

    final PsiDirectory lessonDir = directories[0];
    if (lessonDir == null) {
      return;
    }

    final Lesson lesson = CCUtils.lessonFromDir(course, lessonDir, project);
    if (lesson == null) {
      return;
    }

    if (lesson.getSection() != null && lesson.getSection().getId() <= 0) {
      return;
    }

    if (((RemoteCourse)course).getId() > 0) {
      e.getPresentation().setEnabledAndVisible(true);
      if (lesson.getId() <= 0) {
        e.getPresentation().setText("Upload Lesson to Stepik");
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
    if (!(course instanceof RemoteCourse)) {
      return;
    }
    final PsiDirectory[] directories = view.getDirectories();
    if (directories.length == 0 || directories.length > 1) {
      return;
    }

    final PsiDirectory lessonDir = directories[0];
    if (lessonDir == null || !lessonDir.getName().contains("lesson")) {
      return;
    }

    final Lesson lesson = CCUtils.lessonFromDir(course, lessonDir, project);
    if (lesson == null) {
      return;
    }

    if (CourseExt.getHasSections(course) && lesson.getSection() == null && lesson.getId() <= 0) {
      wrapAndPost(project, course, lesson);
      return;
    }

    ProgressManager.getInstance().run(new Task.Modal(project, "Uploading Lesson", true) {
      @Override
      public void run(@NotNull ProgressIndicator indicator) {
        indicator.setText("Uploading lesson to " + StepikNames.STEPIK_URL);
        doPush(lesson, project, course);
      }
    });
  }

  private static void wrapAndPost(Project project, Course course, Lesson lesson) {
    ApplicationManager.getApplication().invokeAndWait(() -> {
      int result = Messages.showYesNoDialog(project, "Since you have sections, we'll have to wrap this lesson into section before upload",
                                            "Wrap Lesson Into Sections", "Wrap and Post", "Cancel", null);
      if (result == Messages.YES) {
        CCUtils.wrapIntoSection(project, course, Collections.singletonList(lesson), sectionToWrapIntoName(lesson));
        CCPushSection.doPush(project, lesson.getSection(), (RemoteCourse)course);
      }
    });
  }

  @NotNull
  private static String sectionToWrapIntoName(Lesson lesson) {
    return "Section. " + StringUtil.capitalize(lesson.getName());
  }

  // public for tests
  public static void doPush(Lesson lesson, Project project, Course course) {
    if (lesson.getId() > 0) {
      int lessonId = CCStepikConnector.updateLesson(project, lesson, true);
      if (lessonId != -1) {
        CCStepikConnector.showNotification(project, "Lesson updated", CCStepikConnector.seeOnStepikAction("/lesson/" + lessonId));
      }
    }
    else {
      if (CourseExt.getHasSections(course)) {
        Section section = lesson.getSection();
        assert section != null;
        int lessonId = CCStepikConnector.postLesson(project, lesson);
        lesson.unitId = CCStepikConnector.postUnit(lessonId, lesson.getIndex(), section.getId(), project);
      }
      else {
        final int lessonId = CCStepikConnector.postLesson(project, lesson);
        int sectionId;
        final List<Integer> sections = ((RemoteCourse)course).getSectionIds();
        sectionId = sections.get(sections.size() - 1);
        lesson.unitId = CCStepikConnector.postUnit(lessonId, lesson.getIndex(), sectionId, project);
      }
      CCStepikConnector.showNotification(project, "Lesson uploaded", CCStepikConnector.seeOnStepikAction("/lesson/" + lesson.getId()));
    }
  }
}