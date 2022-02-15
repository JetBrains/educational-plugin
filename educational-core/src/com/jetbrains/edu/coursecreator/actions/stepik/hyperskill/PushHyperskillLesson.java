package com.jetbrains.edu.coursecreator.actions.stepik.hyperskill;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task.Modal;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.edu.coursecreator.CCUtils;
import com.jetbrains.edu.coursecreator.actions.CCPluginToggleAction;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.CourseMode;
import com.jetbrains.edu.learning.courseFormat.Lesson;
import com.jetbrains.edu.learning.messages.EduCoreBundle;
import com.jetbrains.edu.learning.stepik.StepikNames;
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse;
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.jetbrains.edu.coursecreator.CCNotificationUtils.showNotification;
import static com.jetbrains.edu.coursecreator.CCUtils.checkIfAuthorizedToStepik;
import static com.jetbrains.edu.coursecreator.stepik.CCStepikConnector.*;
import static com.jetbrains.edu.learning.EduExperimentalFeatures.CC_HYPERSKILL;
import static com.jetbrains.edu.learning.ExperimentsKt.isFeatureEnabled;
import static com.jetbrains.edu.learning.stepik.hyperskill.HyperskillNamesKt.HYPERSKILL;

@SuppressWarnings("ComponentNotRegistered") // Hyperskill.xml
public class PushHyperskillLesson extends DumbAwareAction {
  private static final Logger LOG = Logger.getInstance(PushHyperskillLesson.class);

  @Nls(capitalization = Nls.Capitalization.Sentence)
  public static String getUpdateText() {
    return EduCoreBundle.message("item.update.on.0.lesson.custom", StepikNames.STEPIK, HYPERSKILL);
  }

  @Nls(capitalization = Nls.Capitalization.Title)
  public static String getUpdateTitleText() {
    return EduCoreBundle.message("item.update.on.0.lesson.custom.title", StepikNames.STEPIK, HYPERSKILL);
  }

  @Nls(capitalization = Nls.Capitalization.Sentence)
  public static String getUploadText() {
    return EduCoreBundle.message("item.upload.to.0.lesson.custom", StepikNames.STEPIK, HYPERSKILL);
  }

  @Nls(capitalization = Nls.Capitalization.Title)
  public static String getUploadTitleText() {
    return EduCoreBundle.message("item.upload.to.0.lesson.custom.title", StepikNames.STEPIK, HYPERSKILL);
  }

  public PushHyperskillLesson() {
    super(EduCoreBundle.lazyMessage("gluing.slash", getUploadTitleText(), getUpdateTitleText()),
          EduCoreBundle.lazyMessage("gluing.slash", getUploadText(), getUpdateText()),
          null);
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    e.getPresentation().setEnabledAndVisible(false);
    if (!isFeatureEnabled(CC_HYPERSKILL) || !CCPluginToggleAction.isCourseCreatorFeaturesEnabled()) return;

    final Project project = e.getData(CommonDataKeys.PROJECT);
    if (project == null) return;

    final Course course = StudyTaskManager.getInstance(project).getCourse();
    if (!(course instanceof HyperskillCourse)) {
      return;
    }
    final Lesson lesson = getLesson(e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY), project, course);
    if (lesson == null) return;

    if (lesson.getId() > 0) {
      e.getPresentation().setText(getUpdateTitleText());
    }
    else {
      e.getPresentation().setText(getUploadTitleText());
    }
    e.getPresentation().setEnabledAndVisible(true);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    final Project project = e.getData(CommonDataKeys.PROJECT);
    if (project == null) return;
    if (!checkIfAuthorizedToStepik(project, e.getPresentation().getText())) return;

    final Course course = StudyTaskManager.getInstance(project).getCourse();
    if (!(course instanceof HyperskillCourse)) {
      return;
    }
    final Lesson lesson = getLesson(e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY), project, course);
    if (lesson == null) return;

    ProgressManager.getInstance().run(new Modal(project, EduCoreBundle.message("action.push.custom.lesson.uploading", HYPERSKILL), true) {
      @Override
      public void run(@NotNull ProgressIndicator indicator) {
        indicator.setText(EduCoreBundle.message("action.push.custom.lesson.uploading.to", HYPERSKILL, StepikNames.getStepikUrl()));
        doPush(lesson, project);
        YamlFormatSynchronizer.saveRemoteInfo(lesson);
      }
    });
  }

  @Nullable
  private static Lesson getLesson(@Nullable VirtualFile[] selectedFiles, Project project, Course course) {
    if (!course.getCourseMode().equals(CourseMode.COURSE_MODE)) return null;
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

  public static void doPush(Lesson lesson, Project project) {
    String notification = lesson.getId() > 0 ? EduCoreBundle.message("action.push.custom.lesson.updated", HYPERSKILL)
                                             : EduCoreBundle.message("action.push.custom.lesson.uploaded", HYPERSKILL);
    boolean success = lesson.getId() > 0 ? updateLesson(project, lesson, true, -1)
                                         : postLesson(project, lesson, lesson.getIndex(), -1);

    if (success) {
      showNotification(project, notification, openOnStepikAction("/lesson/" + lesson.getId()));
    }
    else {
      LOG.error("Failed to update Hyperskill lesson");
    }
  }
}