package com.jetbrains.edu.coursecreator.stepik;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
import com.jetbrains.edu.learning.EduNames;
import com.jetbrains.edu.learning.EduSettings;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.configuration.EduConfigurator;
import com.jetbrains.edu.learning.courseFormat.*;
import com.jetbrains.edu.learning.courseFormat.ext.CourseExt;
import com.jetbrains.edu.learning.courseFormat.tasks.ChoiceTask;
import com.jetbrains.edu.learning.courseFormat.tasks.CodeTask;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.stepik.*;
import com.jetbrains.edu.learning.stepik.api.StepikCourseLoader;
import com.jetbrains.edu.learning.stepik.api.StepikMultipleRequestsConnector;
import com.jetbrains.edu.learning.stepik.api.StepikNewConnector;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.event.HyperlinkEvent;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static com.jetbrains.edu.learning.EduUtils.showOAuthDialog;

public class CCStepikConnector {
  private static final Logger LOG = Logger.getInstance(CCStepikConnector.class.getName());
  private static final String FAILED_TITLE = "Failed to publish ";
  private static final String JETBRAINS_USER_ID = "17813950";
  private static final List<Integer> TESTER_USER_IDS = Lists.newArrayList(17869355);
  private static final String PUSH_COURSE_GROUP_ID = "Push.course";

  private CCStepikConnector() {
  }

  public static int getTaskPosition(final int taskId) {
    StepSource step = StepikNewConnector.INSTANCE.getStep(taskId);
    return step != null ? step.getPosition() : -1;
  }

  @Nullable
  public static EduCourse getCourseInfo(@NotNull String courseId) {
    return StepikNewConnector.INSTANCE.getCourseInfo(Integer.valueOf(courseId), null);
  }

  public static void postCourseWithProgress(@NotNull final Project project, @NotNull final Course course) {
    ProgressManager.getInstance().run(new com.intellij.openapi.progress.Task.Modal(project, "Uploading Course", true) {
      @Override
      public void run(@NotNull final ProgressIndicator indicator) {
        postCourse(project, course);
      }
    });
  }

  public static void postCourse(@NotNull final Project project, @NotNull Course course) {
    final StepikUser user = EduSettings.getInstance().getUser();
    if (user == null) {
      showStepikNotification(project, "post course");
      return;
    }

    final ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
    if (indicator != null) {
      indicator.setText("Uploading course to " + StepikNames.STEPIK_URL);
      indicator.setIndeterminate(false);
    }
    final StepikUserInfo currentUser = StepikNewConnector.INSTANCE.getCurrentUserInfo(user);
    if (currentUser != null) {
      final List<StepikUserInfo> courseAuthors = course.getAuthors();
      for (final StepikUserInfo courseAuthor : courseAuthors) {
        currentUser.setFirstName(courseAuthor.getFirstName());
        currentUser.setLastName(courseAuthor.getLastName());
      }
      course.setAuthors(Collections.singletonList(currentUser));
    }

    final EduCourse courseOnRemote = StepikNewConnector.INSTANCE.postCourse(course);
    if (courseOnRemote == null) {
      final String message = FAILED_TITLE + "course " + course.getName();
      LOG.error(message);
      showErrorNotification(project, FAILED_TITLE, message);
      return;
    }
    courseOnRemote.setItems(Lists.newArrayList(course.getItems().stream().filter(it -> !it.getName().equals(EduNames.ADDITIONAL_MATERIALS) &&
                                                                                       !it.getName().equals(StepikNames.PYCHARM_ADDITIONAL)).collect(Collectors.toList())));
    courseOnRemote.setAuthors(course.getAuthors());
    courseOnRemote.setCourseMode(CCUtils.COURSE_MODE);
    courseOnRemote.setLanguage(course.getLanguage());

    if (!ApplicationManager.getApplication().isInternal() && !isTestAccount(currentUser)) {
      addJetBrainsUserAsAdmin(courseOnRemote.adminsGroup);
    }

    int sectionCount;
    if (CourseExt.getHasSections(course)) {
      sectionCount = postSections(project, courseOnRemote);
    }
    else {
      sectionCount = 1;
      postTopLevelLessons(project, courseOnRemote);
    }

    postAdditionalFiles(course, project, courseOnRemote.getId(), sectionCount + 1);
    StudyTaskManager.getInstance(project).setCourse(courseOnRemote);
    courseOnRemote.init(null, null, true);
    StepikUpdateDateExt.setUpdated(courseOnRemote);
    showNotification(project, "Course is published", openOnStepikAction("/course/" + courseOnRemote.getId()));
  }

  private static boolean isTestAccount(@Nullable StepikUserInfo user) {
    return user != null && TESTER_USER_IDS.contains(user.getId());
  }

  private static void addJetBrainsUserAsAdmin(@NotNull String groupId) {
    final int responseCode = StepikNewConnector.INSTANCE.postMember(JETBRAINS_USER_ID, groupId);
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
  private static int postSections(@NotNull Project project, @NotNull EduCourse course) {
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
    return sections.size();
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
      StepikWrappers.Unit unit = StepikNewConnector.INSTANCE.getUnit(topLevelLesson.unitId);
      return unit != null ? unit.getSection() : -1;
    }
    else {
      Course course = StudyTaskManager.getInstance(project).getCourse();
      assert  course != null;
      EduCourse courseInfo = StepikNewConnector.INSTANCE.getCourseInfo(course.getId(), true);
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
    final int sectionId = postSectionInfo(project, copySection(section), course.getId());
    section.setId(sectionId);
    postLessons(project, indicator, course, sectionId, section.getLessons());

    return sectionId;
  }

  public static boolean updateSection(@NotNull Project project, @NotNull Section section) {
    EduCourse course = (EduCourse)StudyTaskManager.getInstance(project).getCourse();
    assert course != null;
    section.setCourseId(course.getId());
    boolean updated = updateSectionInfo(project, section);
    if (updated) {
      for (Lesson lesson : section.getLessons()) {
        if (lesson.getId() > 0) {
          updateLesson(project, lesson, false, section.getId());
        }
        else {
          postLesson(project, lesson, lesson.getIndex(), section.getId());
        }
      }
    }

    return updated;
  }

  public static Section copySection(@NotNull Section section) {
    Section sectionToPost = new Section();
    sectionToPost.setName(section.getName());
    sectionToPost.setPosition(section.getPosition());
    sectionToPost.setId(section.getId());
    sectionToPost.setCourseId(section.getCourseId());

    return sectionToPost;
  }

  private static void postLessons(@NotNull Project project,
                                  @Nullable ProgressIndicator indicator,
                                  @NotNull EduCourse course,
                                  int sectionId,
                                  @NotNull List<Lesson> lessons) {
    int position = 1;
    for (Lesson lesson : lessons) {
      if (indicator != null) {
        indicator.checkCanceled();
        indicator.setText2("Publishing lesson " + lesson.getIndex());
      }
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

  public static void postAdditionalFiles(@NotNull Course course, @NotNull final Project project, int id, int position) {
    final Lesson lesson = CCUtils.createAdditionalLesson(course, project, StepikNames.PYCHARM_ADDITIONAL);
    if (lesson != null) {
      final ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
      if (indicator != null) {
        indicator.setText2("Publishing additional files");
      }
      final Section section = new Section();
      section.setName(StepikNames.PYCHARM_ADDITIONAL);
      section.setPosition(position);
      final int sectionId = postSectionInfo(project, section, id);
      postLesson(project, lesson, position, sectionId);
    }
  }

  public static void updateAdditionalFiles(@NotNull Course course, @NotNull final Project project, Lesson lesson) {
    final Lesson postedLesson = CCUtils.createAdditionalLesson(course, project, StepikNames.PYCHARM_ADDITIONAL);
    if (postedLesson != null) {
      postedLesson.setId(lesson.getId());
      Section section = lesson.getSection();
      assert section != null;
      postedLesson.setSection(section);
      final ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
      if (indicator != null) {
        indicator.setText2("Publishing additional files");
      }

      Lesson updatedLesson = updateLesson(project, postedLesson, false, section.getId());
      if (updatedLesson != null) {
        ((EduCourse)course).setAdditionalMaterialsUpdateDate(updatedLesson.getUpdateDate());
      }
    }
  }

  public static int postUnit(int lessonId, int position, int sectionId, @NotNull Project project) {
    if (!checkIfAuthorized(project, "postUnit")) return lessonId;

    final StepikWrappers.Unit unit = StepikNewConnector.INSTANCE.postUnit(lessonId, position, sectionId);
    if (unit == null) {
      showErrorNotification(project, FAILED_TITLE, "Failed to post unit");
      return -1;
    }
    return unit.id;
  }

  public static void updateUnit(int unitId, int lessonId, int position, int sectionId, @NotNull Project project) {
    if (!checkIfAuthorized(project, "updateUnit")) return;

    final StepikWrappers.Unit unit = StepikNewConnector.INSTANCE.updateUnit(unitId, lessonId, position, sectionId);
    if (unit == null) {
      showErrorNotification(project, FAILED_TITLE, "Failed to update unit");
    }
  }

  public static int postSectionInfo(@NotNull Project project, @NotNull Section section, int courseId) {
    if (!checkIfAuthorized(project, "post section")) return -1;

    section.setCourseId(courseId);
    final Section postedSection = StepikNewConnector.INSTANCE.postSection(section);
    if (postedSection == null) {
      showErrorNotification(project, FAILED_TITLE, "Failed to post section " + section.getId());
      return -1;
    }
    section.setId(postedSection.getId());
    return postedSection.getId();
  }

  public static boolean updateSectionInfo(@NotNull Project project, @NotNull Section section) {
    Section sectionCopy = copySection(section);
    final Section updatedSection = StepikNewConnector.INSTANCE.updateSection(sectionCopy);
    if (updatedSection == null) {
      showErrorNotification(project, FAILED_TITLE, "Failed to post section " + section.getId());
      return false;
    }
    return true;
  }

  public static boolean updateTask(@NotNull final Project project, @NotNull final Task task) {
    if (!checkIfAuthorized(project, "update task")) return false;
    VirtualFile taskDir = task.getTaskDir(project);
    if (taskDir == null) return false;
    final Course course = task.getLesson().getCourse();
    assert course instanceof EduCourse;
    final EduConfigurator configurator = CourseExt.getConfigurator(course);
    if (configurator == null) return false;

    final int responseCode = StepikNewConnector.INSTANCE.updateTask(project, task);

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
    EduCourse courseInfo = getCourseInfo(String.valueOf(course.getId()));
    if (courseInfo != null) {
      course.setPublic(courseInfo.isPublic());
      course.setCompatible(courseInfo.isCompatible());
    }
    else {
      LOG.warn("Failed to get current course info");
    }

    final HttpPut request = new HttpPut(StepikNames.STEPIK_API_URL + StepikNames.COURSES + "/" + course.getId());
    String requestBody = new Gson().toJson(new StepikWrappers.CourseWrapper(course));
    request.setEntity(new StringEntity(requestBody, ContentType.APPLICATION_JSON));
    try {
      final CloseableHttpClient client = StepikAuthorizedClient.getHttpClient();
      if (client == null) {
        LOG.warn("Http client is null");
        return false;
      }
      final CloseableHttpResponse response = client.execute(request);
      final HttpEntity responseEntity = response.getEntity();
      final String responseString = responseEntity != null ? EntityUtils.toString(responseEntity) : "";
      final StatusLine line = response.getStatusLine();
      EntityUtils.consume(responseEntity);
      if (line.getStatusCode() == HttpStatus.SC_FORBIDDEN) {
        showNoRightsToUpdateNotification(project, course);
        return false;
      }
      if (line.getStatusCode() != HttpStatus.SC_OK) {
        final String message = FAILED_TITLE + "course ";
        LOG.warn(message + responseString);
        final String detailString = getErrorDetail(responseString);

        showErrorNotification(project, FAILED_TITLE, detailString);
      }

      return true;
    }
    catch (IOException e) {
      LOG.error(e.getMessage());
      return false;
    }
  }

  public static void updateAdditionalMaterials(@NotNull Project project, int courseId) {
    AtomicBoolean additionalMaterialsUpdated = new AtomicBoolean(false);
    EduCourse courseInfo = getCourseInfo(String.valueOf(courseId));
    assert courseInfo != null;

    List<Integer> sectionIds = courseInfo.getSectionIds();
    for (Integer sectionId : sectionIds) {
      final Section section = StepikNewConnector.INSTANCE.getSection(sectionId);
      if (section != null && StepikNames.PYCHARM_ADDITIONAL.equals(section.getName())) {
        section.setPosition(sectionIds.size());
        section.setId(sectionId);
        updateSectionInfo(project, section);
        final List<Lesson> lessons = StepikCourseLoader.INSTANCE.getLessons(courseInfo, sectionId);
        lessons.stream()
          .filter(Lesson::isAdditional)
          .findFirst()
          .ifPresent(lesson -> {
            lesson.setSection(section);
            updateAdditionalFiles(courseInfo, project, lesson);
            additionalMaterialsUpdated.set(true);
          });
      }
    }
    additionalMaterialsUpdated.get();
  }

  public static boolean updateAdditionalSection(@NotNull Project project) {
    AtomicBoolean additionalMaterialsUpdated = new AtomicBoolean(false);

    EduCourse course = (EduCourse)StudyTaskManager.getInstance(project).getCourse();
    assert course != null;

    EduCourse courseInfo = getCourseInfo(String.valueOf(course.getId()));
    assert courseInfo != null;

    List<Integer> sectionIds = courseInfo.getSectionIds();
    for (Integer sectionId : sectionIds) {
      final Section section = StepikNewConnector.INSTANCE.getSection(sectionId);
      if (section != null && StepikNames.PYCHARM_ADDITIONAL.equals(section.getName())) {
        section.setPosition(sectionIds.size());
        updateSectionInfo(project, section);
      }
    }

    return additionalMaterialsUpdated.get();
  }

  public static Lesson updateLessonInfo(@NotNull final Project project,
                                        @NotNull final Lesson lesson,
                                        boolean showNotification, int sectionId) {
    if (!checkIfAuthorized(project, "update lesson")) return null;
    // TODO: support case when lesson was removed from Stepik

    final Lesson updatedLesson = StepikNewConnector.INSTANCE.updateLesson(lesson);
    if (updatedLesson == null && showNotification) {
      showErrorNotification(project, FAILED_TITLE, "Failed to update lesson " + lesson.getId());
      return null;
    }

    if (!lesson.isAdditional()) {
      updateUnit(lesson.unitId, lesson.getId(), lesson.getIndex(), sectionId, project);
    }

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
      StepikNewConnector.INSTANCE.deleteTask(step);
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
    final Notification notification =
      new Notification(PUSH_COURSE_GROUP_ID, title, message, NotificationType.ERROR);
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

  public static void showNotification(@NotNull Project project,
                                      @NotNull String title,
                                      @Nullable AnAction action) {
    final Notification notification =
      new Notification("Push.course", title, "", NotificationType.INFORMATION);
    if (action != null) {
      notification.addAction(action);
    }
    notification.notify(project);
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
    final Lesson postedLesson = StepikNewConnector.INSTANCE.postLesson(lesson);
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

  private static String getErrorDetail(@NotNull String responseString) {
    final JsonObject details = new JsonParser().parse(responseString).getAsJsonObject();
    final JsonElement detail = details.get("detail");
    return detail != null ? detail.getAsString() : responseString;
  }

  public static boolean postTask(@NotNull final Project project, @NotNull final Task task, final int lessonId) {
    if (!checkIfAuthorized(project, "postTask")) return false;
    if (task instanceof ChoiceTask || task instanceof CodeTask) return false;

    final StepSource stepSource = StepikNewConnector.INSTANCE.postTask(project, task, lessonId);
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
