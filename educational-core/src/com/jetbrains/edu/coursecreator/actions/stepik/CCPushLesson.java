package com.jetbrains.edu.coursecreator.actions.stepik;

import com.intellij.ide.IdeView;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task.Modal;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiDirectory;
import com.jetbrains.edu.coursecreator.CCUtils;
import com.jetbrains.edu.coursecreator.stepik.CCStepikConnector;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseFormat.*;
import com.jetbrains.edu.learning.courseFormat.ext.CourseExt;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.stepik.StepikConnector;
import com.jetbrains.edu.learning.stepik.StepikNames;
import com.jetbrains.edu.learning.stepik.StepikWrappers;
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

    if (course.getId() > 0) {
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

    ProgressManager.getInstance().run(new Modal(project, "Uploading Lesson", true) {
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
        boolean wrapped = CCUtils.wrapIntoSection(project, course, Collections.singletonList(lesson), sectionToWrapIntoName(lesson));
        if (wrapped) {
          Section section = lesson.getSection();
          assert section != null;
          CCPushSection.doPush(project, section, (RemoteCourse)course);
        }
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
      StepikWrappers.Unit unit = StepikConnector.getUnit(lesson.unitId);
      int lessonId = CCStepikConnector.updateLesson(project, lesson, true);
      lesson.setStepikChangeStatus(StepikChangeStatus.UP_TO_DATE);
      setUpdated(lesson);
      if (lessonId != -1) {
        boolean positionChanged = lesson.getIndex() != unit.getPosition();
        if (positionChanged) {
          List<Lesson> lessons = lesson.getSection() != null ? lesson.getSection().getLessons() : course.getLessons();
          updateLessonsPositions(project, 0, lessons);
        }
        CCStepikConnector.showNotification(project, "Lesson updated", CCStepikConnector.openOnStepikAction("/lesson/" + lessonId));
      }
    }
    else {
      if (CourseExt.getHasSections(course)) {
        Section section = lesson.getSection();
        assert section != null;
        int lessonId = CCStepikConnector.postLesson(project, lesson);
        lesson.unitId = CCStepikConnector.postUnit(lessonId, lesson.getIndex(), section.getId(), project);
        if (lesson.getIndex() < section.getLessons().size()) {
          updateLessonsPositions(project,
                                 lesson.getIndex() + 1, section.getLessons().subList(lesson.getIndex(), section.getLessons().size()));
        }
      }
      else {
        final int lessonId = CCStepikConnector.postLesson(project, lesson);
        int sectionId;
        final List<Integer> sections = ((RemoteCourse)course).getSectionIds();
        sectionId = sections.get(sections.size() - 1);
        lesson.unitId = CCStepikConnector.postUnit(lessonId, lesson.getIndex(), sectionId, project);
        if (lesson.getIndex() < course.getLessons().size()) {
          List<Lesson> lessons = course.getLessons();
          updateLessonsPositions(project,
                                 lesson.getIndex() + 1, lessons.subList(lesson.getIndex(), lessons.size()));
        }
      }
      CCStepikConnector.showNotification(project, "Lesson uploaded", CCStepikConnector.openOnStepikAction("/lesson/" + lesson.getId()));
    }
  }

  private static void setUpdated(Lesson lesson) {
    for (Task task : lesson.taskList) {
      task.setStepikChangeStatus(StepikChangeStatus.UP_TO_DATE);
    }
  }

  private static void updateLessonsPositions(@NotNull Project project, int initialPosition, List<Lesson> lessonsToUpdate) {
    int position = initialPosition;
    for (Lesson lesson : lessonsToUpdate) {
      if (lesson.getId() == 0) continue;
      int index = lesson.getIndex();
      lesson.setIndex(position++);
      CCStepikConnector.updateLessonInfo(project, lesson, false);
      lesson.setStepikChangeStatus(StepikChangeStatus.UP_TO_DATE);
      lesson.setIndex(index);
    }
  }
}