package com.jetbrains.edu.coursecreator.actions.stepik.hyperskill;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.Experiments;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task.Modal;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.edu.coursecreator.CCUtils;
import com.jetbrains.edu.coursecreator.actions.CCPluginToggleAction;
import com.jetbrains.edu.coursecreator.stepik.CCStepikConnector;
import com.jetbrains.edu.coursecreator.yaml.YamlFormatSynchronizer;
import com.jetbrains.edu.learning.EduSettings;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.Lesson;
import com.jetbrains.edu.learning.courseFormat.TaskFile;
import com.jetbrains.edu.learning.stepik.StepikNames;
import com.jetbrains.edu.learning.stepik.api.StepikConnector;
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse;
import icons.EducationalCoreIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.jetbrains.edu.coursecreator.stepik.CCStepikConnector.showStepikNotification;
import static com.jetbrains.edu.coursecreator.stepik.CCStepikConnector.updateLesson;
import static com.jetbrains.edu.learning.EduExperimentalFeatures.HYPERSKILL;
import static com.jetbrains.edu.learning.EduUtils.showNotification;

@SuppressWarnings("ComponentNotRegistered") // Hyperskill.xml
public class PushHyperskillLesson extends DumbAwareAction {
  private static final Logger LOG = Logger.getInstance(PushHyperskillLesson.class);

  public PushHyperskillLesson() {
    super("Push Hyperskill Lesson to Stepik", "Push Hyperskill Lesson to Stepik", EducationalCoreIcons.Hyperskill);
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    e.getPresentation().setEnabledAndVisible(false);
    if (!Experiments.isFeatureEnabled(HYPERSKILL) || ! CCPluginToggleAction.isCourseCreatorFeaturesEnabled()) return;

    final Project project = e.getData(CommonDataKeys.PROJECT);
    if (project == null) return;

    final Course course = StudyTaskManager.getInstance(project).getCourse();
    if (!(course instanceof HyperskillCourse)) {
      return;
    }
    final Lesson lesson = getLesson(e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY), project, course);
    if (lesson == null) return;

    if (lesson.getId() > 0) {
      e.getPresentation().setText("Update Hyperskill Lesson on Stepik");
    }
    e.getPresentation().setEnabledAndVisible(true);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    final Project project = e.getData(CommonDataKeys.PROJECT);
    if (project == null) return;
    if (!EduSettings.isLoggedIn()) {
      showStepikNotification(project, "post lesson");
      return;
    }

    final Course course = StudyTaskManager.getInstance(project).getCourse();
    if (!(course instanceof HyperskillCourse)) {
      return;
    }
    final Lesson lesson = getLesson(e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY), project, course);
    if (lesson == null) return;

    ProgressManager.getInstance().run(new Modal(project, "Uploading Hyperskill Lesson", true) {
      @Override
      public void run(@NotNull ProgressIndicator indicator) {
        indicator.setText("Uploading hyperskill lesson to " + StepikNames.STEPIK_URL);
        doPush(lesson, course, project);
        YamlFormatSynchronizer.saveRemoteInfo(lesson);
      }
    });
  }

  @Nullable
  private static Lesson getLesson(@Nullable VirtualFile[] selectedFiles, Project project, Course course) {
    if (!course.getCourseMode().equals(CCUtils.COURSE_MODE)) return null;
    if (selectedFiles == null || selectedFiles.length != 1) {
      return null;
    }

    VirtualFile lessonDir = selectedFiles[0];
    if (!lessonDir.isDirectory()) {
      return null;
    }

    final Lesson lesson = CCUtils.lessonFromDir(course, lessonDir, project);
    if (lesson == null) {
      return null;
    }
    return lesson;
  }

  public static void doPush(Lesson lesson, Course course, Project project) {
    if (lesson.getId() > 0) {
      boolean success = updateLesson(project, lesson, true, -1);
      if (success) {
        final List<TaskFile> additionalFiles = CCUtils.collectAdditionalFiles(course, project);
        StepikConnector.updateLessonAttachment(additionalFiles, lesson.getId());
        showNotification(project, "Hyperskill lesson updated", CCStepikConnector.openOnStepikAction("/lesson/" + lesson.getId()));
      }
      else {
        LOG.error("Failed to update hyperskill lesson");
      }
    }
    else {
      CCStepikConnector.postLesson(project, lesson, lesson.getIndex(), -1);
      final List<TaskFile> additionalFiles = CCUtils.collectAdditionalFiles(course, project);
      StepikConnector.postAttachment(additionalFiles, null, lesson.getId());
      showNotification(project, "Lesson uploaded", CCStepikConnector.openOnStepikAction("/lesson/" + lesson.getId()));
    }
  }
}