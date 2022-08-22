package com.jetbrains.edu.coursecreator.stepik;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.edu.coursecreator.AdditionalFilesUtils;
import com.jetbrains.edu.coursecreator.StudyItemType;
import com.jetbrains.edu.coursecreator.StudyItemTypeKt;
import com.jetbrains.edu.learning.EduBrowser;
import com.jetbrains.edu.learning.OpenApiExtKt;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.ItemContainer;
import com.jetbrains.edu.learning.courseFormat.Lesson;
import com.jetbrains.edu.learning.courseFormat.ext.StudyItemExtKt;
import com.jetbrains.edu.learning.courseFormat.tasks.CodeTask;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.messages.EduCoreBundle;
import com.jetbrains.edu.learning.stepik.StepSource;
import com.jetbrains.edu.learning.stepik.StepikNames;
import com.jetbrains.edu.learning.stepik.api.LessonAdditionalInfo;
import com.jetbrains.edu.learning.stepik.api.StepikConnector;
import com.jetbrains.edu.learning.stepik.api.StepikUnit;
import com.jetbrains.edu.learning.stepik.course.StepikLesson;
import org.apache.http.HttpStatus;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.jetbrains.edu.coursecreator.CCNotificationUtils.*;
import static com.jetbrains.edu.coursecreator.CCUtils.checkIfAuthorizedToStepik;
import static com.jetbrains.edu.learning.stepik.StepikNames.STEPIK;

public class CCStepikConnector {
  private static final Logger LOG = Logger.getInstance(CCStepikConnector.class.getName());

  private CCStepikConnector() { }

  // POST methods:

  public static boolean postLesson(@NotNull final Project project, @NotNull final Lesson lesson, int position, int sectionId) {
    Lesson postedLesson = postLessonInfo(project, lesson, sectionId, position);
    if (postedLesson == null) return false;
    postedLesson.setIndex(lesson.getIndex());
    postedLesson.setItems(lesson.getItems());
    postedLesson.setParent(lesson.getParent());

    boolean success = true;
    for (Task task : lesson.getTaskList()) {
      checkCanceled();
      success = postTask(project, task, postedLesson.getId()) && success;
    }
    if (!updateLessonAdditionalInfo(lesson, project)) {
      showFailedToPostItemNotification(project, lesson, true);
      return false;
    }
    ItemContainer parent = lesson.getParent();
    parent.removeItem(lesson);
    parent.addItem(lesson.getIndex() - 1, postedLesson);

    return success;
  }

  public static Lesson postLessonInfo(@NotNull Project project, @NotNull Lesson lesson, int sectionId, int position) {
    if (!checkIfAuthorizedToStepik(project, StudyItemTypeKt.getUploadToStepikTitleMessage(StudyItemType.LESSON_TYPE))) return null;
    Course course = StudyTaskManager.getInstance(project).getCourse();
    assert course != null;
    final StepikLesson postedLesson = StepikConnector.getInstance().postLesson(lesson);
    if (postedLesson == null) {
      showFailedToPostItemNotification(project, lesson, true);
      return null;
    }
    if (sectionId != -1) {
      postedLesson.setUnitId(postUnit(postedLesson.getId(), position, sectionId, project));
    }

    return postedLesson;
  }

  public static int postUnit(int lessonId, int position, int sectionId, @NotNull Project project) {
    if (!checkIfAuthorizedToStepik(project, StudyItemTypeKt.getUploadToStepikTitleMessage(StudyItemType.LESSON_TYPE))) return lessonId;

    final StepikUnit unit = StepikConnector.getInstance().postUnit(lessonId, position, sectionId);
    if (unit == null || unit.getId() == null) {
      showErrorNotification(project, EduCoreBundle.message("course.creator.stepik.failed.to.post.unit"));
      return -1;
    }
    return unit.getId();
  }

  public static boolean postTask(@NotNull final Project project, @NotNull final Task task, final int lessonId) {
    if (!checkIfAuthorizedToStepik(project, StudyItemTypeKt.getUploadToStepikTitleMessage(StudyItemType.TASK_TYPE))) return false;
    // TODO: add meaningful comment to final Success notification that Code tasks were not pushed
    if (task instanceof CodeTask) return true;

    final StepSource stepSource = StepikConnector.getInstance().postTask(project, task, lessonId);
    if (stepSource == null) {
      showFailedToPostItemNotification(project, task, true);
      return false;
    }
    task.setId(stepSource.getId());
    task.setUpdateDate(stepSource.getUpdateDate());
    return true;
  }

  // UPDATE methods:

  public static boolean updateLesson(@NotNull final Project project,
                                     @NotNull final Lesson lesson,
                                     boolean showNotification,
                                     int sectionId) {
    StepikLesson postedLesson = updateLessonInfo(project, lesson, showNotification, sectionId);
    return postedLesson != null &&
           updateLessonTasks(project, lesson, postedLesson.getStepIds()) &&
           updateLessonAdditionalInfo(lesson, project);
  }

  public static StepikLesson updateLessonInfo(@NotNull final Project project,
                                              @NotNull final Lesson lesson,
                                              boolean showNotification, int sectionId) {
    if (!checkIfAuthorizedToStepik(project, StudyItemTypeKt.getUpdateOnStepikTitleMessage(StudyItemType.LESSON_TYPE))) return null;
    // TODO: support case when lesson was removed from Stepik

    final StepikLesson updatedLesson = StepikConnector.getInstance().updateLesson(lesson);
    if (updatedLesson == null) {
      if (showNotification) {
        showFailedToPostItemNotification(project, lesson, false);
      }
      return null;
    }
    if (sectionId != -1) {
      updateUnit(updatedLesson.getUnitId(), lesson.getId(), lesson.getIndex(), sectionId, project);
    }

    return updatedLesson;
  }

  public static boolean updateLessonAdditionalInfo(@NotNull final Lesson lesson, @NotNull Project project) {
    if (!checkIfAuthorizedToStepik(project, StudyItemTypeKt.getUpdateOnStepikTitleMessage(StudyItemType.LESSON_TYPE))) return false;

    LessonAdditionalInfo info = AdditionalFilesUtils.collectAdditionalLessonInfo(lesson, project);
    if (info.isEmpty()) {
      StepikConnector.getInstance().deleteLessonAttachment(lesson.getId());
      return true;
    }
    updateProgress(EduCoreBundle.message("course.creator.stepik.progress.details.publishing.additional.data", lesson.getPresentableName()));
    return StepikConnector.getInstance().updateLessonAttachment(info, lesson) == HttpStatus.SC_CREATED;
  }

  public static void updateUnit(int unitId, int lessonId, int position, int sectionId, @NotNull Project project) {
    if (!checkIfAuthorizedToStepik(project, StudyItemTypeKt.getUpdateOnStepikTitleMessage(StudyItemType.LESSON_TYPE))) return;

    final StepikUnit unit = StepikConnector.getInstance().updateUnit(unitId, lessonId, position, sectionId);
    if (unit == null) {
      showErrorNotification(project, EduCoreBundle.message("course.creator.stepik.failed.to.update.unit"));
    }
  }

  private static boolean updateLessonTasks(@NotNull Project project, @NotNull Lesson localLesson, @NotNull List<Integer> steps) {
    final Set<Integer> localTasksIds = localLesson.getTaskList()
      .stream()
      .map(task -> task.getId())
      .filter(id -> id > 0)
      .collect(Collectors.toSet());

    final List<Integer> taskIdsToDelete = steps.stream()
      .filter(id -> !localTasksIds.contains(id))
      .collect(Collectors.toList());

    // Remove all tasks from Stepik which are not in our lessons now
    for (Integer step : taskIdsToDelete) {
      StepikConnector.getInstance().deleteTask(step);
    }

    boolean success = true;
    for (Task task : localLesson.getTaskList()) {
      checkCanceled();
      success = (task.getId() > 0 ? updateTask(project, task) : postTask(project, task, localLesson.getId())) && success;
    }
    return success;
  }

  public static boolean updateTask(@NotNull final Project project, @NotNull final Task task) {
    if (!checkIfAuthorizedToStepik(project, StudyItemTypeKt.getUpdateOnStepikTitleMessage(StudyItemType.TASK_TYPE))) return false;
    VirtualFile taskDir = StudyItemExtKt.getDir(task, OpenApiExtKt.getCourseDir(project));
    if (taskDir == null) return false;

    final int responseCode = StepikConnector.getInstance().updateTask(project, task);

    switch (responseCode) {
      case HttpStatus.SC_OK:
        StepSource step = StepikConnector.getInstance().getStep(task.getId());
        if (step != null) {
          task.setUpdateDate(step.getUpdateDate());
        }
        else {
          LOG.warn(String.format("Failed to get step for task '%s' with id %d while setting an update date", task.getName(), task.getId()));
        }
        return true;
      case HttpStatus.SC_NOT_FOUND:
        // TODO: support case when lesson was removed from Stepik too
        return postTask(project, task, task.getLesson().getId());
      case HttpStatus.SC_FORBIDDEN:
        showNoRightsToUpdateOnStepikNotification(project);
        return false;
      default:
        showFailedToPostItemNotification(project, task, false);
        return false;
    }
  }

  // helper methods:

  @SuppressWarnings("UnstableApiUsage")
  private static void updateProgress(@NlsContexts.ProgressDetails @NotNull String text) {
    final ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
    if (indicator != null) {
      indicator.checkCanceled();
      indicator.setText2(text);
    }
  }

  public static AnAction openOnStepikAction(@NotNull @NonNls String url) {
    return new AnAction(EduCoreBundle.message("action.open.on.text", STEPIK)) {
      @Override
      public void actionPerformed(@NotNull AnActionEvent e) {
        EduBrowser.getInstance().browse(StepikNames.getStepikUrl() + url);
      }
    };
  }

  private static void checkCanceled() {
    final ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
    if (indicator != null) {
      indicator.checkCanceled();
    }
  }
}
