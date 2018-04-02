package com.jetbrains.edu.coursecreator.stepik;

import com.google.common.collect.Lists;
import com.google.gson.*;
import com.intellij.lang.Language;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.edu.coursecreator.CCUtils;
import com.jetbrains.edu.learning.EduConfigurator;
import com.jetbrains.edu.learning.EduConfiguratorManager;
import com.jetbrains.edu.learning.EduSettings;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseFormat.*;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.serialization.SerializationUtils;
import com.jetbrains.edu.learning.stepik.*;
import com.jetbrains.edu.learning.stepik.serialization.StepikSubmissionAnswerPlaceholderAdapter;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static com.jetbrains.edu.learning.EduUtils.showOAuthDialog;

public class CCStepikConnector {
  private static final Logger LOG = Logger.getInstance(CCStepikConnector.class.getName());
  private static final String FAILED_TITLE = "Failed to publish ";

  private CCStepikConnector() {
  }

  @Nullable
  public static RemoteCourse getCourseInfo(@NotNull String courseId) {
    final String url = StepikNames.COURSES + "/" + courseId;
    final StepicUser user = EduSettings.getInstance().getUser();
    try {
      final StepikWrappers.CoursesContainer coursesContainer = StepikConnector.getCoursesFromStepik(user, url);
      return coursesContainer == null ? null : coursesContainer.courses.get(0);
    }
    catch (IOException e) {
      LOG.warn(e.getMessage());
    }
    return null;
  }

  public static void postCourseWithProgress(@NotNull final Project project, @NotNull final Course course) {
    ProgressManager.getInstance().run(new com.intellij.openapi.progress.Task.Modal(project, "Uploading Course", true) {
      @Override
      public void run(@NotNull final ProgressIndicator indicator) {
        postCourse(project, course);
      }
    });
  }

  private static void postCourse(@NotNull final Project project, @NotNull Course course) {
    if (!checkIfAuthorized(project, "post course")) return;

    final ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
    if (indicator != null) {
      indicator.setText("Uploading course to " + StepikNames.STEPIK_URL);
      indicator.setIndeterminate(false);
    }
    final HttpPost request = new HttpPost(StepikNames.STEPIK_API_URL + "/courses");

    final StepicUser currentUser = StepikAuthorizedClient.getCurrentUser();
    if (currentUser != null) {
      final List<StepicUser> courseAuthors = course.getAuthors();
      for (int i = 0; i < courseAuthors.size(); i++) {
        if (courseAuthors.size() > i) {
          final StepicUser courseAuthor = courseAuthors.get(i);
          currentUser.setFirstName(courseAuthor.getFirstName());
          currentUser.setLastName(courseAuthor.getLastName());
        }
      }
      course.setAuthors(Collections.singletonList(currentUser));
    }

    String requestBody = new Gson().toJson(new StepikWrappers.CourseWrapper(course));
    request.setEntity(new StringEntity(requestBody, ContentType.APPLICATION_JSON));

    try {
      final CloseableHttpClient client = StepikAuthorizedClient.getHttpClient();
      if (client == null) {
        LOG.warn("Http client is null");
        return;
      }
      final CloseableHttpResponse response = client.execute(request);
      final HttpEntity responseEntity = response.getEntity();
      final String responseString = responseEntity != null ? EntityUtils.toString(responseEntity) : "";
      final StatusLine line = response.getStatusLine();
      EntityUtils.consume(responseEntity);
      if (line.getStatusCode() != HttpStatus.SC_CREATED) {
        final String message = FAILED_TITLE + "course ";
        LOG.error(message + responseString);
        showErrorNotification(project, FAILED_TITLE, responseString);
        return;
      }
      final RemoteCourse courseOnRemote = new Gson().fromJson(responseString, StepikWrappers.CoursesContainer.class).courses.get(0);
      courseOnRemote.setItems(Lists.newArrayList(course.getItems()));
      courseOnRemote.setAuthors(course.getAuthors());
      courseOnRemote.setCourseMode(CCUtils.COURSE_MODE);
      courseOnRemote.setLanguage(course.getLanguageID());

      int sectionCount = postSections(project, courseOnRemote);

      ApplicationManager.getApplication().invokeAndWait(() -> FileDocumentManager.getInstance().saveAllDocuments());
      final int finalSectionCount = sectionCount;
      ApplicationManager.getApplication().runReadAction(() -> postAdditionalFiles(course, project, courseOnRemote.getId(), finalSectionCount + 1));
      StudyTaskManager.getInstance(project).setCourse(courseOnRemote);
      showNotification(project, "Course published");
    }
    catch (IOException e) {
      LOG.error(e.getMessage());
    }
  }

  private static int postSections(@NotNull Project project, @NotNull RemoteCourse course) {
    final ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
    course.sortItems();
    final List<StudyItem> items = course.getItems();
    int i = 0;
    for (StudyItem item : items) {
      final Section section = new Section();
      List<Lesson> lessons;
      if (item instanceof Section) {
        ((Section)item).setPosition(i + 1);
        section.setName(item.getName());
        lessons = ((Section)item).getLessons();
      }
      else {
        section.setName(item.getName());
        lessons = Collections.singletonList((Lesson)item);
      }

      section.setPosition(i + 1);
      final int sectionId = postModule(course.getId(), section, project);
      section.setId(sectionId);

      int position = 1;
      for (Lesson lesson : lessons) {
        if (indicator != null) {
          indicator.checkCanceled();
          indicator.setText2("Publishing lesson " + lesson.getIndex());
        }
        final int lessonId = postLesson(project, lesson);
        postUnit(lessonId, position, sectionId, project);
        if (indicator != null) {
          indicator.setFraction((double)lesson.getIndex() / course.getLessons().size());
          indicator.checkCanceled();
        }
        position += 1;
      }
      i += 1;
    }
    return items.size();
  }

  private static boolean checkIfAuthorized(@NotNull Project project, @NotNull String failedActionName) {
    boolean isAuthorized = EduSettings.getInstance().getUser() != null;
    if (!isAuthorized) {
      showStepikNotification(project, NotificationType.ERROR, failedActionName);
      return false;
    }
    return true;
  }

  public static void postAdditionalFiles(Course course, @NotNull final Project project, int id, int position) {
    final Lesson lesson = CCUtils.createAdditionalLesson(course, project, StepikNames.PYCHARM_ADDITIONAL);
    if (lesson != null) {
      final ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
      if (indicator != null) {
        indicator.setText2("Publishing additional files");
      }
      final Section section = new Section();
      section.setName(StepikNames.PYCHARM_ADDITIONAL);
      section.setPosition(position);
      final int sectionId = postModule(id, section, project);
      final int lessonId = postLesson(project, lesson);
      postUnit(lessonId, 1, sectionId, project);
    }
  }

  public static void updateAdditionalFiles(Course course, @NotNull final Project project, int stepikId) {
    final Lesson lesson = CCUtils.createAdditionalLesson(course, project, StepikNames.PYCHARM_ADDITIONAL);
    if (lesson != null) {
      lesson.setId(stepikId);
      final ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
      if (indicator != null) {
        indicator.setText2("Publishing additional files");
      }
      updateLesson(project, lesson);
    }
  }

  public static void postUnit(int lessonId, int position, int sectionId, Project project) {
    if (!checkIfAuthorized(project, "postTask")) return;

    final HttpPost request = new HttpPost(StepikNames.STEPIK_API_URL + StepikNames.UNITS);
    final StepikWrappers.UnitWrapper unitWrapper = new StepikWrappers.UnitWrapper();
    final StepikWrappers.Unit unit = new StepikWrappers.Unit();
    unit.setLesson(lessonId);
    unit.setPosition(position);
    unit.setSection(sectionId);
    unitWrapper.setUnit(unit);

    String requestBody = new Gson().toJson(unitWrapper);
    request.setEntity(new StringEntity(requestBody, ContentType.APPLICATION_JSON));

    try {
      final CloseableHttpClient client = StepikAuthorizedClient.getHttpClient();
      if (client == null) return;
      final CloseableHttpResponse response = client.execute(request);
      final HttpEntity responseEntity = response.getEntity();
      final String responseString = responseEntity != null ? EntityUtils.toString(responseEntity) : "";
      final StatusLine line = response.getStatusLine();
      EntityUtils.consume(responseEntity);
      if (line.getStatusCode() != HttpStatus.SC_CREATED) {
        LOG.error(FAILED_TITLE + responseString);
        showErrorNotification(project, FAILED_TITLE, responseString);
      }
    }
    catch (IOException e) {
      LOG.error(e.getMessage());
    }
  }

  private static int postModule(int courseId, Section section, Project project) {
    final HttpPost request = new HttpPost(StepikNames.STEPIK_API_URL + StepikNames.SECTIONS);
    section.setCourse(courseId);
    final StepikWrappers.SectionWrapper sectionContainer = new StepikWrappers.SectionWrapper();
    sectionContainer.setSection(section);
    String requestBody = new Gson().toJson(sectionContainer);
    request.setEntity(new StringEntity(requestBody, ContentType.APPLICATION_JSON));

    try {
      final CloseableHttpClient client = StepikAuthorizedClient.getHttpClient();
      if (client == null) return -1;
      final CloseableHttpResponse response = client.execute(request);
      final HttpEntity responseEntity = response.getEntity();
      final String responseString = responseEntity != null ? EntityUtils.toString(responseEntity) : "";
      final StatusLine line = response.getStatusLine();
      EntityUtils.consume(responseEntity);
      if (line.getStatusCode() != HttpStatus.SC_CREATED) {
        LOG.error(FAILED_TITLE + responseString);
        showErrorNotification(project, FAILED_TITLE, responseString);
        return -1;
      }
      final Section postedSection = new Gson().fromJson(responseString, StepikWrappers.SectionContainer.class).getSections().get(0);
      return postedSection.getId();
    }
    catch (IOException e) {
      LOG.error(e.getMessage());
    }
    return -1;
  }

  public static void updateTask(@NotNull final Project project, @NotNull final Task task) {
    if (!checkIfAuthorized(project, "update task")) return;
    final Lesson lesson = task.getLesson();
    final int lessonId = lesson.getId();

    VirtualFile taskDir = task.getTaskDir(project);
    if (taskDir == null) return;

    final HttpPut request = new HttpPut(StepikNames.STEPIK_API_URL + StepikNames.STEP_SOURCES
                                                                                    + String.valueOf(task.getStepId()));
    final Gson gson = new GsonBuilder().setPrettyPrinting().excludeFieldsWithoutExposeAnnotation().
      registerTypeAdapter(AnswerPlaceholder.class, new StepikSubmissionAnswerPlaceholderAdapter()).create();
    ApplicationManager.getApplication().invokeLater(() -> {
      final Language language = lesson.getCourse().getLanguageById();
      final EduConfigurator configurator = EduConfiguratorManager.forLanguage(language);
      if (configurator == null) return;
      List<VirtualFile> testFiles = Arrays.stream(taskDir.getChildren()).filter(configurator::isTestFile)
                                                 .collect(Collectors.toList());
      for (VirtualFile file : testFiles) {
        try {
          task.addTestsTexts(file.getName(), VfsUtilCore.loadText(file));
        }
        catch (IOException e) {
          LOG.warn("Failed to load text " + file.getName());
        }
      }
      final String requestBody = gson.toJson(new StepikWrappers.StepSourceWrapper(project, task, lessonId));
      request.setEntity(new StringEntity(requestBody, ContentType.APPLICATION_JSON));

      try {
        final CloseableHttpClient client = StepikAuthorizedClient.getHttpClient();
        if (client == null) return;
        final CloseableHttpResponse response = client.execute(request);
        final HttpEntity responseEntity = response.getEntity();
        final String responseString = responseEntity != null ? EntityUtils.toString(responseEntity) : "";
        EntityUtils.consume(responseEntity);
        final StatusLine line = response.getStatusLine();
        switch (line.getStatusCode()) {
          case HttpStatus.SC_OK:
            showNotification(project, "Task updated");
            break;
          case HttpStatus.SC_NOT_FOUND:
            // TODO: support case when lesson was removed from Stepik too
            postTask(project, task, task.getLesson().getId());
            break;
          default:
            final String message = "Failed to update task ";
            LOG.error(message + responseString);
            showErrorNotification(project, message, responseString);
        }
      }
      catch (IOException e) {
        LOG.error(e.getMessage());
      }
    });
  }

  public static void updateCourse(@NotNull final Project project, @NotNull final RemoteCourse course) {
    if (!checkIfAuthorized(project, "update course")) return;

    // Course info can be changed from Stepik site directly
    // so we get actual info here
    RemoteCourse courseInfo = getCourseInfo(String.valueOf(course.getId()));
    if (courseInfo != null) {
      course.setName(courseInfo.getName());
      course.setDescription(courseInfo.getDescription());
      course.setPublic(courseInfo.isPublic());
    } else {
      LOG.warn("Failed to get current course info");
    }

    final HttpPut request = new HttpPut(StepikNames.STEPIK_API_URL + StepikNames.COURSES + "/" + String.valueOf(course.getId()));
    String requestBody = new Gson().toJson(new StepikWrappers.CourseWrapper(course));
    request.setEntity(new StringEntity(requestBody, ContentType.APPLICATION_JSON));
    try {
      final CloseableHttpClient client = StepikAuthorizedClient.getHttpClient();
      if (client == null) {
        LOG.warn("Http client is null");
        return;
      }
      final CloseableHttpResponse response = client.execute(request);
      final HttpEntity responseEntity = response.getEntity();
      final String responseString = responseEntity != null ? EntityUtils.toString(responseEntity) : "";
      final StatusLine line = response.getStatusLine();
      EntityUtils.consume(responseEntity);
      if (line.getStatusCode() != HttpStatus.SC_OK) {
        final String message = FAILED_TITLE + "course ";
        LOG.error(message + responseString);
        showErrorNotification(project, FAILED_TITLE, responseString);
      }
      final RemoteCourse postedCourse = new Gson().fromJson(responseString, StepikWrappers.CoursesContainer.class).courses.get(0);
      updateLessons(course, project);
      ApplicationManager.getApplication().invokeAndWait(() -> FileDocumentManager.getInstance().saveAllDocuments());
      final List<Integer> sectionIds = postedCourse.getSectionIds();
      if (!updateAdditionalMaterials(project, course, sectionIds)) {
        postAdditionalFiles(course, project, course.getId(), sectionIds.size());
      }
    } catch (IOException e) {
      LOG.error(e.getMessage());
    }
  }

  private static boolean updateAdditionalMaterials(@NotNull Project project, @NotNull final RemoteCourse course,
                                                   @NotNull final List<Integer> sectionsIds) throws IOException {
    AtomicBoolean additionalMaterialsUpdated = new AtomicBoolean(false);
    for (Integer sectionId : sectionsIds) {
      final Section section = StepikConnector.getSection(sectionId);
      if (section != null && StepikNames.PYCHARM_ADDITIONAL.equals(section.getName())) {
        final List<Lesson> lessons = StepikConnector.getLessons(course, sectionId);
        lessons.stream()
                .filter(Lesson::isAdditional)
                .findFirst()
                .ifPresent(lesson -> {
                        updateAdditionalFiles(course, project, lesson.getId());
                        additionalMaterialsUpdated.set(true);
                });
      }
    }
    return additionalMaterialsUpdated.get();
  }

  private static void updateLessons(Course course, Project project) {
    final ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
    for (Lesson lesson : course.getLessons()) {
      indicator.checkCanceled();
      indicator.setText2("Publishing lesson " + lesson.getIndex());

      if (lesson.getId() > 0) {
        updateLesson(project, lesson);
      }
      else {
        final int lessonId = postLesson(project, lesson);
        if (lessonId != -1) {
          final List<Integer> sectionIds = ((RemoteCourse)course).getSectionIds();
          final Integer sectionId = sectionIds.get(sectionIds.size() - 1);
          postUnit(lessonId, lesson.getIndex(), sectionId, project);
        }
      }
      indicator.setFraction((double)lesson.getIndex()/course.getLessons().size());
    }
  }

  public static int updateLesson(@NotNull final Project project, @NotNull final Lesson lesson) {
    if(!checkIfAuthorized(project, "update lesson")) return -1;

    final HttpPut request = new HttpPut(StepikNames.STEPIK_API_URL + StepikNames.LESSONS + String.valueOf(lesson.getId()));

    String requestBody = new Gson().toJson(new StepikWrappers.LessonWrapper(lesson));
    request.setEntity(new StringEntity(requestBody, ContentType.APPLICATION_JSON));

    try {
      final CloseableHttpClient client = StepikAuthorizedClient.getHttpClient();
      if (client == null) return -1;
      final CloseableHttpResponse response = client.execute(request);
      final HttpEntity responseEntity = response.getEntity();
      final String responseString = responseEntity != null ? EntityUtils.toString(responseEntity) : "";
      final StatusLine line = response.getStatusLine();
      EntityUtils.consume(responseEntity);
      // TODO: support case when lesson was removed from Stepik
      if (line.getStatusCode() != HttpStatus.SC_OK) {
        final String message = "Failed to update lesson ";
        LOG.error(message + responseString);
        showErrorNotification(project, message, responseString);
        return -1;
      }

      final Lesson postedLesson = getLessonFromString(responseString);
      if (postedLesson == null) {
        return -1;
      }
      updateLessonTasks(project, lesson, postedLesson);
      return lesson.getId();
    }
    catch (IOException e) {
      LOG.error(e.getMessage());
    }
    return -1;
  }

  private static void updateLessonTasks(@NotNull Project project, @NotNull Lesson localLesson, @NotNull Lesson remoteLesson) {
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
      deleteTask(step, project);
    }

    for (Task task : localLesson.getTaskList()) {
      final ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
      if (indicator != null) {
        indicator.checkCanceled();
      }
      if (task.getStepId() > 0) {
        updateTask(project, task);
      } else {
        postTask(project, task, localLesson.getId());
      }
    }
  }

  private static void showErrorNotification(@NotNull Project project, String message, String responseString) {
    final JsonObject details = new JsonParser().parse(responseString).getAsJsonObject();
    final JsonElement detail = details.get("detail");
    final String detailString = detail != null ? detail.getAsString() : responseString;
    final Notification notification =
      new Notification("Push.course", message, detailString, NotificationType.ERROR);
    notification.notify(project);
  }

  public static void showNotification(@NotNull Project project, String message) {
    final Notification notification =
      new Notification("Push.course", message, message, NotificationType.INFORMATION);
    notification.notify(project);
  }

  private static void showStepikNotification(@NotNull Project project,
                                             @NotNull NotificationType notificationType, @NotNull String failedActionName) {
    String text = "Log in to Stepik to " + failedActionName;
    Notification notification = new Notification("Stepik", "Failed to " + failedActionName, text, notificationType);
    notification.addAction(new AnAction("Log in") {

      @Override
      public void actionPerformed(AnActionEvent e) {
        StepikConnector.doAuthorize(() -> showOAuthDialog());
        notification.expire();
      }
    });

    notification.notify(project);
  }

  public static int postLesson(@NotNull final Project project, @NotNull final Lesson lesson) {
    if (!checkIfAuthorized(project, "postLesson")) return -1;

    final HttpPost request = new HttpPost(StepikNames.STEPIK_API_URL + "/lessons");

    String requestBody = new Gson().toJson(new StepikWrappers.LessonWrapper(lesson));
    request.setEntity(new StringEntity(requestBody, ContentType.APPLICATION_JSON));

    try {
      final CloseableHttpClient client = StepikAuthorizedClient.getHttpClient();
      if (client == null) return -1;
      final CloseableHttpResponse response = client.execute(request);
      final HttpEntity responseEntity = response.getEntity();
      final String responseString = responseEntity != null ? EntityUtils.toString(responseEntity) : "";
      final StatusLine line = response.getStatusLine();
      EntityUtils.consume(responseEntity);
      if (line.getStatusCode() != HttpStatus.SC_CREATED) {
        final String message = FAILED_TITLE + "lesson ";
        LOG.error(message + responseString);
        showErrorNotification(project, message, responseString);
        return 0;
      }

      final Lesson postedLesson = getLessonFromString(responseString);
      if (postedLesson == null) {
        return -1;
      }
      lesson.setId(postedLesson.getId());
      for (Task task : lesson.getTaskList()) {
        final ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
        if (indicator != null) {
          indicator.checkCanceled();
        }
        postTask(project, task, postedLesson.getId());
      }
      return postedLesson.getId();
    }
    catch (IOException e) {
      LOG.error(e.getMessage());
    }
    return -1;
  }

  @Nullable
  public static Lesson getLessonFromString(@NotNull String responseString) {
    final JsonObject jsonTree = new Gson().fromJson(responseString, JsonObject.class);
    if (jsonTree.has(SerializationUtils.LESSONS)) {
      final JsonArray lessons = jsonTree.get(SerializationUtils.LESSONS).getAsJsonArray();
      if (lessons.size() == 1) {
        return new Gson().fromJson(lessons.get(0), Lesson.class);
      }
    }
    return null;
  }

  public static void deleteTask(@NotNull final Integer task, Project project) {
    final HttpDelete request = new HttpDelete(StepikNames.STEPIK_API_URL + StepikNames.STEP_SOURCES + task);
    ApplicationManager.getApplication().invokeLater(() -> {
      try {
        final CloseableHttpClient client = StepikAuthorizedClient.getHttpClient();
        if (client == null) return;
        final CloseableHttpResponse response = client.execute(request);
        final HttpEntity responseEntity = response.getEntity();
        final String responseString = responseEntity != null ? EntityUtils.toString(responseEntity) : "";
        EntityUtils.consume(responseEntity);
        final StatusLine line = response.getStatusLine();
        if (line.getStatusCode() != HttpStatus.SC_NO_CONTENT) {
          LOG.error("Failed to delete task " + responseString);
          showErrorNotification(project, "Failed to delete task ", responseString);
        }
      }
      catch (IOException e) {
        LOG.error(e.getMessage());
      }
    });
  }

  public static void postTask(final Project project, @NotNull final Task task, final int lessonId) {
    if (!checkIfAuthorized(project, "postTask")) return;

    final HttpPost request = new HttpPost(StepikNames.STEPIK_API_URL + "/step-sources");
    final Gson gson = new GsonBuilder().setPrettyPrinting().excludeFieldsWithoutExposeAnnotation().
      registerTypeAdapter(AnswerPlaceholder.class, new StepikSubmissionAnswerPlaceholderAdapter()).create();
    ApplicationManager.getApplication().invokeLater(() -> {
      final String requestBody = gson.toJson(new StepikWrappers.StepSourceWrapper(project, task, lessonId));
      request.setEntity(new StringEntity(requestBody, ContentType.APPLICATION_JSON));

      try {
        final CloseableHttpClient client = StepikAuthorizedClient.getHttpClient();
        if (client == null) return;
        final CloseableHttpResponse response = client.execute(request);
        final StatusLine line = response.getStatusLine();
        final HttpEntity responseEntity = response.getEntity();
        final String responseString = responseEntity != null ? EntityUtils.toString(responseEntity) : "";
        EntityUtils.consume(responseEntity);
        if (line.getStatusCode() != HttpStatus.SC_CREATED) {
          final String message = FAILED_TITLE + "task ";
          LOG.error(message + responseString);
          showErrorNotification(project, message, responseString);
          return;
        }

        final JsonObject postedTask = new Gson().fromJson(responseString, JsonObject.class);
        final JsonObject stepSource = postedTask.getAsJsonArray("step-sources").get(0).getAsJsonObject();
        task.setStepId(stepSource.getAsJsonPrimitive("id").getAsInt());
      }
      catch (IOException e) {
        LOG.error(e.getMessage());
      }
    });
  }
}
