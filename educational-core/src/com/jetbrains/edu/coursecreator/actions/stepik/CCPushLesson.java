package com.jetbrains.edu.coursecreator.actions.stepik;

import com.intellij.ide.IdeView;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task.Modal;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiDirectory;
import com.jetbrains.edu.coursecreator.CCUtils;
import com.jetbrains.edu.coursecreator.stepik.CCStepikConnector;
import com.jetbrains.edu.coursecreator.yaml.YamlFormatSynchronizer;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseFormat.*;
import com.jetbrains.edu.learning.courseFormat.ext.CourseExt;
import com.jetbrains.edu.learning.stepik.StepikNames;
import com.jetbrains.edu.learning.stepik.api.StepikConnector;
import com.jetbrains.edu.learning.stepik.api.StepikUnit;
import com.twelvemonkeys.lang.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

@SuppressWarnings("ComponentNotRegistered") // educational-core.xml
public class CCPushLesson extends DumbAwareAction {
  private static final Logger LOG = Logger.getInstance(CCPushLesson.class);

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
    if (!(course instanceof EduCourse) || !((EduCourse)course).isRemote()) {
      return;
    }
    if (!course.getCourseMode().equals(CCUtils.COURSE_MODE)) return;
    final PsiDirectory[] directories = view.getDirectories();
    if (directories.length != 1) {
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

    if (lesson.getSection() == null && ((EduCourse)course).getSectionIds().isEmpty()) {
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
    if (!(course instanceof EduCourse) || !((EduCourse)course).isRemote()) {
      return;
    }
    final PsiDirectory[] directories = view.getDirectories();
    if (directories.length != 1) {
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

    if (CourseExt.getHasSections(course) && lesson.getSection() == null && lesson.getId() <= 0) {
      wrapAndPost(project, course, lesson);
      return;
    }

    ProgressManager.getInstance().run(new Modal(project, "Uploading Lesson", true) {
      @Override
      public void run(@NotNull ProgressIndicator indicator) {
        indicator.setText("Uploading lesson to " + StepikNames.STEPIK_URL);
        doPush(lesson, project, (EduCourse)course);
        YamlFormatSynchronizer.saveRemoteInfo(lesson);
      }
    });
  }

  private static void wrapAndPost(Project project, Course course, Lesson lesson) {
    ApplicationManager.getApplication().invokeAndWait(() -> {
      int result = Messages.showYesNoDialog(project, "Since you have sections, we'll have to wrap this lesson into section before upload",
                                            "Wrap Lesson Into Sections", "Wrap and Post", "Cancel", null);
      if (result == Messages.YES) {
        Section section = CCUtils.wrapIntoSection(project, course, Collections.singletonList(lesson), sectionToWrapIntoName(lesson));
        if (section != null) {
          CCPushSection.doPush(project, section, (EduCourse)course);
          YamlFormatSynchronizer.saveRemoteInfo(section);
        }
      }
    });
  }

  @NotNull
  private static String sectionToWrapIntoName(Lesson lesson) {
    return "Section. " + StringUtil.capitalize(lesson.getName());
  }

  // public for tests
  public static void doPush(Lesson lesson, Project project, EduCourse course) {
    if (lesson.getId() > 0) {
      StepikUnit unit = StepikConnector.getUnit(lesson.unitId);
      if (unit == null) {
        LOG.error("Failed to get unit for unit id " + lesson.unitId);
        return;
      }
      int sectionId;
      if (lesson.getSection() != null) {
        sectionId = lesson.getSection().getId();
      }
      else {
        sectionId = course.getSectionIds().get(0);
      }
      boolean success = CCStepikConnector.updateLesson(project, lesson, true, sectionId);
      if (success) {
        boolean positionChanged = lesson.getIndex() != unit.getPosition();
        if (positionChanged) {
          List<Lesson> lessons = lesson.getSection() != null ? lesson.getSection().getLessons() : course.getLessons();
          updateLessonsPositions(project, 0, lessons);
        }
        EduUtils.showNotification(project, "Lesson updated", CCStepikConnector.openOnStepikAction("/lesson/" + lesson.getId()));
      }
    }
    else {
      if (CourseExt.getHasSections(course)) {
        Section section = lesson.getSection();
        assert section != null;
        int position = lessonPosition(section, lesson);
        CCStepikConnector.postLesson(project, lesson, lesson.getIndex(), section.getId());
        if (lesson.getIndex() < section.getLessons().size()) {
          updateLessonsPositions(project, position + 1, section.getLessons());
        }
      }
      else {
        int position = lessonPosition(course, lesson);
        int sectionId;
        final List<Integer> sections = course.getSectionIds();
        sectionId = sections.get(sections.size() - 1);
        CCStepikConnector.postLesson(project, lesson, lesson.getIndex(), sectionId);
        if (lesson.getIndex() < course.getLessons().size()) {
          List<Lesson> lessons = course.getLessons();
          updateLessonsPositions(project, position + 1, lessons);
        }
      }
      EduUtils.showNotification(project, "Lesson uploaded", CCStepikConnector.openOnStepikAction("/lesson/" + lesson.getId()));
    }
  }

  private static void updateLessonsPositions(@NotNull Project project, int initialPosition, List<Lesson> lessons) {
    int position = 1;

    // update positions for posted lessons only
    for (Lesson lesson : lessons) {
      // skip unpushed lessons
      if (lesson.getId() == 0) {
        continue;
      }

      // skip lessons before target
      if (position < initialPosition) {
        continue;
      }
      int index = lesson.getIndex();
      lesson.setIndex(position++);
      int sectionId;
      if (lesson.getSection() != null) {
        sectionId = lesson.getSection().getId();
      }
      else {
        EduCourse course = (EduCourse)StudyTaskManager.getInstance(project).getCourse();
        assert course != null;
        sectionId = course.getSectionIds().get(0);
      }
      CCStepikConnector.updateLessonInfo(project, lesson, false, sectionId);
      lesson.setIndex(index);
    }
  }

  private static int lessonPosition(@NotNull ItemContainer parent, @NotNull Lesson lesson) {
    int position = 1;
    for (StudyItem item : parent.getItems()) {
      if (item.getId() == 0) {
        continue;
      }

      if (item.getName().equals(lesson.getName())) {
        continue;
      }

      position++;
    }

    return position;
  }
}