package com.jetbrains.edu.coursecreator.stepik;

import com.google.common.collect.Lists;
import com.intellij.ide.BrowserUtil;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.edu.coursecreator.CCUtils;
import com.jetbrains.edu.learning.EduSettings;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.configuration.EduConfigurator;
import com.jetbrains.edu.learning.courseFormat.*;
import com.jetbrains.edu.learning.courseFormat.ext.CourseExt;
import com.jetbrains.edu.learning.courseFormat.tasks.ChoiceTask;
import com.jetbrains.edu.learning.courseFormat.tasks.CodeTask;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.stepik.*;
import com.jetbrains.edu.learning.stepik.api.StepikConnector;
import com.jetbrains.edu.learning.stepik.api.StepikMultipleRequestsConnector;
import com.jetbrains.edu.learning.stepik.api.StepikUnit;
import org.apache.http.HttpStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.event.HyperlinkEvent;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.jetbrains.edu.learning.EduUtils.showNotification;
import static com.jetbrains.edu.learning.EduUtils.showOAuthDialog;

public class CCStepikConnector {
  private static final Logger LOG = Logger.getInstance(CCStepikConnector.class.getName());
  public static final String FAILED_TITLE = "Failed to publish ";
  public static final String PUSH_COURSE_GROUP_ID = "Push.course";
  private static final String JETBRAINS_USER_ID = "17813950";
  private static final List<Integer> TESTER_USER_IDS = Lists.newArrayList(17869355);

  private CCStepikConnector() {
  }

  public static int getTaskPosition(final int taskId) {
    StepSource step = StepikConnector.getStep(taskId);
    return step != null ? step.getPosition() : -1;
  }

  public static void postCourseWithProgress(@NotNull final Project project, @NotNull final EduCourse course) {
    ProgressManager.getInstance().run(new com.intellij.openapi.progress.Task.Modal(project, "Uploading Course", true) {
      @Override
      public void run(@NotNull final ProgressIndicator indicator) {
        indicator.setIndeterminate(false);
        postCourse(project, course);
      }
    });
  }

  public static void postCourse(@NotNull final Project project, @NotNull EduCourse course) {
    final StepikUser user = EduSettings.getInstance().getUser();
    if (user == null) {
      showStepikNotification(project, "post course");
      return;
    }
    updateProgress("Uploading course to " + StepikNames.STEPIK_URL);
    final StepikUserInfo currentUser = StepikConnector.getCurrentUserInfo(user);
    if (currentUser != null) {
      final List<StepikUserInfo> courseAuthors = course.getAuthors();
      for (final StepikUserInfo courseAuthor : courseAuthors) {
        currentUser.setFirstName(courseAuthor.getFirstName());
        currentUser.setLastName(courseAuthor.getLastName());
      }
      course.setAuthors(Collections.singletonList(currentUser));
    }

    final EduCourse courseOnRemote = StepikConnector.postCourse(course);
    if (courseOnRemote == null) {
      final String message = FAILED_TITLE + "course " + course.getName();
      LOG.error(message);
      showErrorNotification(project, FAILED_TITLE, message);
      return;
    }
    final List<StudyItem> items = course.getItems();
    courseOnRemote.setItems(Lists.newArrayList(items));
    courseOnRemote.setAuthors(course.getAuthors());
    courseOnRemote.setCourseMode(CCUtils.COURSE_MODE);
    courseOnRemote.setLanguage(course.getLanguage());

    if (!ApplicationManager.getApplication().isInternal() && !isTestAccount(currentUser)) {
      addJetBrainsUserAsAdmin(courseOnRemote.getAdminsGroup());
    }

    if (CourseExt.getHasSections(course)) {
      postSections(project, courseOnRemote);
    }
    else {
      postTopLevelLessons(project, courseOnRemote);
    }

    postAdditionalFiles(course, project, courseOnRemote.getId());
    StudyTaskManager.getInstance(project).setCourse(courseOnRemote);
    courseOnRemote.init(null, null, true);
    StepikUpdateDateExt.setUpdated(courseOnRemote);
    showNotification(project, "Course is published", openOnStepikAction("/course/" + courseOnRemote.getId()));
  }

  private static boolean isTestAccount(@Nullable StepikUserInfo user) {
    return user != null && TESTER_USER_IDS.contains(user.getId());
  }

  private static void addJetBrainsUserAsAdmin(@NotNull String groupId) {
    final int responseCode = StepikConnector.postMember(JETBRAINS_USER_ID, groupId);
    if (responseCode != HttpStatus.SC_CREATED) {
      LOG.warn("Failed to add JetBrains as admin ");
    }
  }

  public static void wrapUnpushedLessonsIntoSections(@NotNull Project project, @NotNull Course course) {
    ApplicationManager.getApplication().invokeAndWait(() -> {
      List<Lesson> lessons = course.getLessons();
      for (Lesson lesson : lessons) {
        if (lesson.getId() > 0) {
          continue;
        }
        CCUtils.wrapIntoSection(project, course, Collections.singletonList(lesson), "Section. " + StringUtil.capitalize(lesson.getName()));
      }
    });
  }

  /**
   * This method should be used for courses with sections only
   */
  private static void postSections(@NotNull Project project, @NotNull EduCourse course) {
    final ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
    course.sortItems();
    final List<Section> sections = course.getSections();
    assert course.getLessons().isEmpty() : "postSections method should be used for courses with sections only";
    int i = 1;
    for (Section item : sections) {
      Section section = new Section();
      section.setPosition(i++);
      section.setName(item.getName());
      List<Lesson> lessons = item.getLessons();

      final int sectionId = postSectionInfo(project, section, course.getId());
      item.setId(sectionId);

      postLessons(project, indicator, course, sectionId, lessons);
    }
  }

  private static void postTopLevelLessons(@NotNull Project project, @NotNull EduCourse course) {
    final ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
    final int sectionId = postSectionForTopLevelLessons(project, course);
    course.setSectionIds(Collections.singletonList(sectionId));
    postLessons(project, indicator, course, sectionId, course.getLessons());
  }

  public static int postSectionForTopLevelLessons(@NotNull Project project, @NotNull EduCourse course) {
    Section section = new Section();
    section.setName(course.getName());
    section.setPosition(1);
    return postSectionInfo(project, section, course.getId());
  }

  public static int findTopLevelLessonsSection(@NotNull Project project, @Nullable Lesson topLevelLesson) {
    if (topLevelLesson != null) {
      StepikUnit unit = StepikConnector.getUnit(topLevelLesson.unitId);
      return unit != null ? unit.getSection() : -1;
    }
    else {
      Course course = StudyTaskManager.getInstance(project).getCourse();
      assert course != null;
      EduCourse courseInfo = StepikConnector.getCourseInfo(course.getId(), true);
      if (courseInfo != null) {
        List<Integer> sectionIds = courseInfo.getSectionIds();
        List<Section> sections = StepikMultipleRequestsConnector.INSTANCE.getSections(sectionIds);
        for (Section section : sections) {
          if (section.getName().equals(courseInfo.getName())) {
            return section.getId();
          }
        }
      }
      else {
        LOG.error(String.format("Course with id %s not found on Stepik", course.getId()));
      }
    }

    return -1;
  }

  public static int postSection(@NotNull Project project, @NotNull Section section, @Nullable ProgressIndicator indicator) {
    EduCourse course = (EduCourse)StudyTaskManager.getInstance(project).getCourse();
    assert course != null;
    final int sectionId = postSectionInfo(project, section, course.getId());
    section.setId(sectionId);
    postLessons(project, indicator, course, sectionId, section.getLessons());

    return sectionId;
  }

  public static boolean updateSection(@NotNull Section section, @NotNull Course course, @NotNull Project project) {
    section.setCourseId(course.getId());
    final Section updatedSection = updateSectionInfo(section);
    if (updatedSection == null) {
      showErrorNotification(project, FAILED_TITLE, "Failed to update section " + section.getId());
      return false;
    }
    for (Lesson lesson : section.getLessons()) {
      if (lesson.getId() > 0) {
        updateLesson(project, lesson, false, section.getId());
      }
      else {
        postLesson(project, lesson, lesson.getIndex(), section.getId());
      }
    }

    return true;
  }

  private static void postLessons(@NotNull Project project,
                                  @Nullable ProgressIndicator indicator,
                                  @NotNull EduCourse course,
                                  int sectionId,
                                  @NotNull List<Lesson> lessons) {
    int position = 1;
    for (Lesson lesson : lessons) {
      updateProgress("Publishing lesson " + lesson.getIndex());
      postLesson(project, lesson, position, sectionId);
      if (indicator != null) {
        indicator.setFraction((double)lesson.getIndex() / course.getLessons().size());
        indicator.checkCanceled();
      }
      position += 1;
    }
  }

  private static boolean checkIfAuthorized(@NotNull Project project, @NotNull String failedActionName) {
    if (!EduSettings.isLoggedIn()) {
      showStepikNotification(project, failedActionName);
      return false;
    }
    return true;
  }

  public static void postAdditionalFiles(@NotNull EduCourse course, @NotNull final Project project, int id) {
    updateProgress("Publishing additional files");
    final List<TaskFile> additionalFiles = CCUtils.collectAdditionalFiles(course, project);
    StepikConnector.postAttachment(additionalFiles, id);
  }

  private static void updateProgress(@NotNull String text) {
    final ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
    if (indicator != null) {
      indicator.checkCanceled();
      indicator.setText2(text);
    }
  }

  public static int postUnit(int lessonId, int position, int sectionId, @NotNull Project project) {
    if (!checkIfAuthorized(project, "postUnit")) return lessonId;

    final StepikUnit unit = StepikConnector.postUnit(lessonId, position, sectionId);
    if (unit == null) {
      showErrorNotification(project, FAILED_TITLE, "Failed to post unit");
      return -1;
    }
    return unit.getId() != null ? unit.getId() : -1;
  }

  public static void updateUnit(int unitId, int lessonId, int position, int sectionId, @NotNull Project project) {
    if (!checkIfAuthorized(project, "updateUnit")) return;

    final StepikUnit unit = StepikConnector.updateUnit(unitId, lessonId, position, sectionId);
    if (unit == null) {
      showErrorNotification(project, FAILED_TITLE, "Failed to update unit");
    }
  }

  public static int postSectionInfo(@NotNull Project project, @NotNull Section section, int courseId) {
    if (!checkIfAuthorized(project, "post section")) return -1;

    section.setCourseId(courseId);
    final Section postedSection = StepikConnector.postSection(section);
    if (postedSection == null) {
      showErrorNotification(project, FAILED_TITLE, "Failed to post section " + section.getId());
      return -1;
    }
    section.setId(postedSection.getId());
    return postedSection.getId();
  }

  public static Section updateSectionInfo(@NotNull Section section) {
    section.units.clear();
    return StepikConnector.updateSection(section);
  }

  public static boolean updateTask(@NotNull final Project project, @NotNull final Task task) {
    if (!checkIfAuthorized(project, "update task")) return false;
    VirtualFile taskDir = task.getTaskDir(project);
    if (taskDir == null) return false;
    final Course course = task.getLesson().getCourse();
    assert course instanceof EduCourse;
    final EduConfigurator configurator = CourseExt.getConfigurator(course);
    if (configurator == null) return false;

    final int responseCode = StepikConnector.updateTask(project, task);

    switch (responseCode) {
      case HttpStatus.SC_OK:
        return true;
      case HttpStatus.SC_NOT_FOUND:
        // TODO: support case when lesson was removed from Stepik too
        return postTask(project, task, task.getLesson().getId());
      case HttpStatus.SC_FORBIDDEN:
        showNoRightsToUpdateNotification(project, (EduCourse)course);
        return false;
      default:
        final String message = "Failed to update task " + task.getStepId();
        LOG.error(message);
        return false;
    }
  }

  public static boolean updateCourseInfo(@NotNull final Project project, @NotNull final EduCourse course) {
    if (!checkIfAuthorized(project, "update course")) return false;
    // Course info parameters such as isPublic() and isCompatible can be changed from Stepik site only
    // so we get actual info here
    EduCourse courseInfo = StepikConnector.getCourseInfo(course.getId());
    if (courseInfo != null) {
      course.setPublic(courseInfo.isPublic());
      course.setCompatible(courseInfo.isCompatible());
    }
    else {
      LOG.warn("Failed to get current course info");
    }
    int responseCode = StepikConnector.updateCourse(course);

    if (responseCode == HttpStatus.SC_FORBIDDEN) {
      showNoRightsToUpdateNotification(project, course);
      return false;
    }
    if (responseCode != HttpStatus.SC_OK) {
      final String message = FAILED_TITLE + "course " + course.getId();
      LOG.warn(message);
      showErrorNotification(project, FAILED_TITLE, message);
      return false;
    }
    return true;
  }

  public static int updateAdditionalMaterials(@NotNull Project project, int courseId) {
    EduCourse courseInfo = StepikConnector.getCourseInfo(courseId);
    assert courseInfo != null;
    updateProgress("Publishing additional files");
    final List<TaskFile> additionalFiles = CCUtils.collectAdditionalFiles(courseInfo, project);
    return StepikConnector.updateAttachment(additionalFiles, courseInfo);
  }

  public static Lesson updateLessonInfo(@NotNull final Project project,
                                        @NotNull final Lesson lesson,
                                        boolean showNotification, int sectionId) {
    if (!checkIfAuthorized(project, "update lesson")) return null;
    // TODO: support case when lesson was removed from Stepik

    final Lesson updatedLesson = StepikConnector.updateLesson(lesson);
    if (updatedLesson == null && showNotification) {
      showErrorNotification(project, FAILED_TITLE, "Failed to update lesson " + lesson.getId());
      return null;
    }
    updateUnit(lesson.unitId, lesson.getId(), lesson.getIndex(), sectionId, project);

    return updatedLesson;
  }

  public static Lesson updateLesson(@NotNull final Project project,
                                    @NotNull final Lesson lesson,
                                    boolean showNotification, int sectionId) {
    Lesson postedLesson = updateLessonInfo(project, lesson, showNotification, sectionId);

    if (postedLesson != null) {
      updateLessonTasks(project, lesson, postedLesson);
      return postedLesson;
    }

    return null;
  }

  private static void updateLessonTasks(@NotNull Project project,
                                        @NotNull Lesson localLesson,
                                        @NotNull Lesson remoteLesson) {
    final Set<Integer> localTasksIds = localLesson.getTaskList()
      .stream()
      .map(task -> task.getStepId())
      .filter(id -> id > 0)
      .collect(Collectors.toSet());

    final List<Integer> taskIdsToDelete = remoteLesson.steps.stream()
      .filter(id -> !localTasksIds.contains(id))
      .collect(Collectors.toList());

    // Remove all tasks from Stepik which are not in our lessons now
    for (Integer step : taskIdsToDelete) {
      StepikConnector.deleteTask(step);
    }

    for (Task task : localLesson.getTaskList()) {
      checkCancelled();
      if (task.getStepId() > 0) {
        updateTask(project, task);
      }
      else {
        postTask(project, task, localLesson.getId());
      }
    }
  }

  public static void showErrorNotification(@NotNull Project project, @NotNull String title, @NotNull String message) {
    final Notification notification = new Notification(PUSH_COURSE_GROUP_ID, title, message, NotificationType.ERROR);
    notification.notify(project);
  }

  private static void showNoRightsToUpdateNotification(@NotNull final Project project, @NotNull final EduCourse course) {
    String message = "You don't have permission to update the course <br> <a href=\"upload\">Upload to Stepik as New Course</a>";
    Notification notification = new Notification(PUSH_COURSE_GROUP_ID, FAILED_TITLE, message, NotificationType.ERROR,
                                                 createPostCourseNotificationListener(project, course));
    notification.notify(project);
  }

  @NotNull
  public static NotificationListener.Adapter createPostCourseNotificationListener(@NotNull Project project,
                                                                                  @NotNull EduCourse course) {
    return new NotificationListener.Adapter() {
      @Override
      protected void hyperlinkActivated(@NotNull Notification notification, @NotNull HyperlinkEvent e) {
        notification.expire();
        course.convertToLocal();
        postCourseWithProgress(project, course);
      }
    };
  }

  public static AnAction openOnStepikAction(@NotNull String url) {
    return new AnAction("Open on Stepik") {
      @Override
      public void actionPerformed(@NotNull AnActionEvent e) {
        BrowserUtil.browse(StepikNames.STEPIK_URL + url);
      }
    };
  }

  private static void showStepikNotification(@NotNull Project project,
                                             @NotNull String failedActionName) {
    String text = "Log in to Stepik to " + failedActionName;
    Notification notification = new Notification("Stepik", "Failed to " + failedActionName, text, NotificationType.ERROR);
    notification.addAction(new DumbAwareAction("Log in") {

      @Override
      public void actionPerformed(@NotNull AnActionEvent e) {
        StepikAuthorizer.doAuthorize(() -> showOAuthDialog());
        notification.expire();
      }
    });

    notification.notify(project);
  }

  public static void postLesson(@NotNull final Project project, @NotNull final Lesson lesson, int position, int sectionId) {
    Lesson postedLesson = postLessonInfo(project, lesson, sectionId, position);

    if (postedLesson == null) {
      return;
    }
    lesson.setId(postedLesson.getId());
    lesson.unitId = postedLesson.unitId;
    for (Task task : lesson.getTaskList()) {
      final ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
      if (indicator != null) {
        indicator.checkCanceled();
      }
      postTask(project, task, postedLesson.getId());
    }
  }

  public static Lesson postLessonInfo(@NotNull Project project, @NotNull Lesson lesson, int sectionId, int position) {
    if (!checkIfAuthorized(project, "postLesson")) return null;
    Course course = StudyTaskManager.getInstance(project).getCourse();
    assert course != null;
    final Lesson postedLesson = StepikConnector.postLesson(lesson);
    if (postedLesson == null) {
      final String message = FAILED_TITLE + "lesson " + lesson.getId();
      showErrorNotification(project, FAILED_TITLE, message);
      return null;
    }
    postedLesson.unitId = postUnit(postedLesson.getId(), position, sectionId, project);

    return postedLesson;
  }

  private static void checkCancelled() {
    final ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
    if (indicator != null) {
      indicator.checkCanceled();
    }
  }

  public static boolean postTask(@NotNull final Project project, @NotNull final Task task, final int lessonId) {
    if (!checkIfAuthorized(project, "postTask")) return false;
    if (task instanceof ChoiceTask || task instanceof CodeTask) return false;

    final StepSource stepSource = StepikConnector.postTask(project, task, lessonId);
    if (stepSource == null) {
      final String message = FAILED_TITLE + "task in lesson " + lessonId;
      LOG.error(message);
      showErrorNotification(project, FAILED_TITLE, message);
      return false;
    }
    task.setStepId(stepSource.getId());
    return true;
  }

  public static int getTopLevelSectionId(@NotNull Project project, @NotNull EduCourse course) {
    if (!course.getSectionIds().isEmpty()) {
      return course.getSectionIds().get(0);
    }
    else {
      Lesson topLevelLesson = getTopLevelLesson(course);
      if (topLevelLesson == null) {
        LOG.warn("Failed to find top-level lesson for a course: " + course.getId());
        return -1;
      }

      int id = findTopLevelLessonsSection(project, topLevelLesson);
      if (id != -1) {
        return id;
      }
      else {
        return postSectionForTopLevelLessons(project, course);
      }
    }
  }

  @Nullable
  private static Lesson getTopLevelLesson(EduCourse course) {
    for (Lesson lesson : course.getLessons()) {
      if (lesson.getStepikChangeStatus() == StepikChangeStatus.UP_TO_DATE) {
        return lesson;
      }
    }

    return null;
  }
}
