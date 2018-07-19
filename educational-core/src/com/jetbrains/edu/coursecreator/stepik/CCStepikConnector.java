package com.jetbrains.edu.coursecreator.stepik;

import com.google.common.collect.Lists;
import com.google.gson.*;
import com.intellij.ide.BrowserUtil;
import com.intellij.lang.Language;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.xmlb.XmlSerializer;
import com.jetbrains.edu.coursecreator.CCUtils;
import com.jetbrains.edu.learning.EduConfigurator;
import com.jetbrains.edu.learning.EduConfiguratorManager;
import com.jetbrains.edu.learning.EduSettings;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseFormat.*;
import com.jetbrains.edu.learning.courseFormat.ext.CourseExt;
import com.jetbrains.edu.learning.courseFormat.tasks.ChoiceTask;
import com.jetbrains.edu.learning.courseFormat.tasks.CodeTask;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.serialization.SerializationUtils;
import com.jetbrains.edu.learning.stepik.*;
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

import javax.swing.event.HyperlinkEvent;
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
  private static final String JETBRAINS_USER_ID = "17813950";
  private static final List<Integer> TESTER_USER_IDS = Lists.newArrayList(17869355);
  private static final String PUSH_COURSE_GROUP_ID = "Push.course";

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
    final HttpPost request = new HttpPost(StepikNames.STEPIK_API_URL + StepikNames.COURSES);

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
        final String detailString = getErrorDetail(responseString);

        showErrorNotification(project, FAILED_TITLE, detailString);
        return;
      }
      final RemoteCourse courseOnRemote = new Gson().fromJson(responseString, StepikWrappers.CoursesContainer.class).courses.get(0);
      courseOnRemote.setItems(Lists.newArrayList(course.getItems()));
      courseOnRemote.setAuthors(course.getAuthors());
      courseOnRemote.setCourseMode(CCUtils.COURSE_MODE);
      courseOnRemote.setLanguage(course.getLanguageID());

      if (!ApplicationManager.getApplication().isInternal() && !isTestAccount(currentUser)) {
        addJetBrainsUserAsAdmin(client, getAdminsGroupId(responseString));
      }
      int sectionCount;
      if (CourseExt.getHasSections(course)) {
        sectionCount = postSections(project, courseOnRemote);
      }
      else {
        sectionCount = 1;
        postTopLevelLessons(project, courseOnRemote);
      }

      ApplicationManager.getApplication().invokeAndWait(() -> FileDocumentManager.getInstance().saveAllDocuments());
      postAdditionalFiles(course, project, courseOnRemote.getId(), sectionCount + 1);
      StudyTaskManager.getInstance(project).setCourse(courseOnRemote);
      courseOnRemote.init(null, null, true);
      courseOnRemote.setUpdated();
      showNotification(project, "Course is published", openOnStepikAction("/course/" + courseOnRemote.getId()));
    }
    catch (IOException e) {
      LOG.error(e.getMessage());
    }
  }

  private static boolean isTestAccount(@Nullable StepicUser user) {
    return user != null && TESTER_USER_IDS.contains(user.getId());
  }

  private static void addJetBrainsUserAsAdmin(@NotNull CloseableHttpClient client, @NotNull String groupId) {
    JsonObject object = new JsonObject();
    JsonObject member = new JsonObject();
    member.addProperty("group", groupId);
    member.addProperty("user", JETBRAINS_USER_ID);
    object.add("member", member);

    HttpPost post = new HttpPost(StepikNames.STEPIK_API_URL + StepikNames.MEMBERS);
    post.setEntity(new StringEntity(object.toString(), ContentType.APPLICATION_JSON));
    try {
      final CloseableHttpResponse response = client.execute(post);
      final HttpEntity responseEntity = response.getEntity();
      final String responseString = responseEntity != null ? EntityUtils.toString(responseEntity) : "";
      final StatusLine line = response.getStatusLine();
      EntityUtils.consume(responseEntity);
      if (line.getStatusCode() != HttpStatus.SC_CREATED) {
        LOG.warn("Failed to add JetBrains as admin " + responseString);
      }
    }
    catch (IOException e) {
      LOG.warn(e.getMessage());
    }
  }

  private static String getAdminsGroupId(String responseString) {
    JsonObject coursesObject = new JsonParser().parse(responseString).getAsJsonObject();
    return coursesObject.get("courses").getAsJsonArray().get(0).getAsJsonObject().get("admins_group").getAsString();
  }

  public static void wrapUnpushedLessonsIntoSections(Project project, Course course) {
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

  private static int postSections(@NotNull Project project, @NotNull RemoteCourse course) {
    final ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
    course.sortItems();
    final List<StudyItem> items = course.getItems();
    int i = 1;
    for (StudyItem item : items) {
      Section section = new Section();
      section.setPosition(i++);
      section.setName(item.getName());
      List<Lesson> lessons = ((Section)item).getLessons();

      final int sectionId = postModule(project, section, course.getId());
      ((Section)item).setId(sectionId);

      postLessons(project, indicator, course, sectionId, lessons);
    }
    return items.size();
  }

  private static void postTopLevelLessons(@NotNull Project project, @NotNull RemoteCourse course) {
    final ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
    Section section = new Section();
    section.setName(course.getName());
    section.setPosition(1);
    final int sectionId = postModule(project, section, course.getId());
    course.setSectionIds(Collections.singletonList(sectionId));
    postLessons(project, indicator, course, sectionId, course.getLessons());
  }

  public static int postSection(@NotNull Project project, @NotNull Section section, @Nullable ProgressIndicator indicator) {
    RemoteCourse course = (RemoteCourse)StudyTaskManager.getInstance(project).getCourse();
    assert course != null;
    final int sectionId = postModule(project, copySection(section), course.getId());
    section.setId(sectionId);
    postLessons(project, indicator, course, sectionId, section.getLessons());

    return sectionId;
  }

  public static boolean updateSection(@NotNull Project project, @NotNull Section section) {
    RemoteCourse course = (RemoteCourse)StudyTaskManager.getInstance(project).getCourse();
    assert course != null;
    section.setCourse(course.getId());
    boolean updated = updateSectionInfo(project, section);
    if (updated) {
      for (Lesson lesson : section.getLessons()) {
        if (lesson.getId() > 0) {
          updateLesson(project, lesson, false);
        }
        else {
          postLesson(project, lesson);
          lesson.unitId = postUnit(lesson.getId(), lesson.getIndex(), section.getId(), project);
        }
      }
    }

    return updated;
  }

  private static Section copySection(@NotNull Section section) {
    Section sectionToPost = new Section();
    sectionToPost.setName(section.getName());
    sectionToPost.setPosition(section.getPosition());
    sectionToPost.setId(section.getId());
    sectionToPost.setCourse(section.getCourse());

    return sectionToPost;
  }

  private static void postLessons(@NotNull Project project,
                                  @Nullable ProgressIndicator indicator,
                                  RemoteCourse course,
                                  int sectionId,
                                  List<Lesson> lessons) {
    int position = 1;
    for (Lesson lesson : lessons) {
      if (indicator != null) {
        indicator.checkCanceled();
        indicator.setText2("Publishing lesson " + lesson.getIndex());
      }
      final int lessonId = postLesson(project, lesson);
      lesson.unitId = postUnit(lessonId, position, sectionId, project);
      if (indicator != null) {
        indicator.setFraction((double)lesson.getIndex() / course.getLessons().size());
        indicator.checkCanceled();
      }
      position += 1;
    }
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
      final int sectionId = postModule(project, section, id);
      final int lessonId = postLesson(project, lesson);
      lesson.unitId = postUnit(lessonId, position, sectionId, project);
    }
  }

  public static void updateAdditionalFiles(Course course, @NotNull final Project project, Lesson lesson) {
    final Lesson postedLesson = CCUtils.createAdditionalLesson(course, project, StepikNames.PYCHARM_ADDITIONAL);
    if (postedLesson != null) {
      postedLesson.setId(lesson.getId());
      postedLesson.setSection(lesson.getSection());
      final ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
      if (indicator != null) {
        indicator.setText2("Publishing additional files");
      }

      Lesson updatedLesson = updateLesson(project, postedLesson, false);
      if (updatedLesson != null) {
        ((RemoteCourse)course).setAdditionalMaterialsUpdateDate(updatedLesson.getUpdateDate());
      }
    }
  }

  public static int postUnit(int lessonId, int position, int sectionId, Project project) {
    if (!checkIfAuthorized(project, "postUnit")) return lessonId;

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
      if (client == null) return lessonId;
      final CloseableHttpResponse response = client.execute(request);
      final HttpEntity responseEntity = response.getEntity();
      final String responseString = responseEntity != null ? EntityUtils.toString(responseEntity) : "";
      final StatusLine line = response.getStatusLine();
      EntityUtils.consume(responseEntity);
      if (line.getStatusCode() != HttpStatus.SC_CREATED) {
        LOG.error(FAILED_TITLE + responseString);
        final String detailString = getErrorDetail(responseString);

        showErrorNotification(project, FAILED_TITLE, detailString);
      }
      else {
        StepikWrappers.UnitContainer unitContainer = new Gson().fromJson(responseString, StepikWrappers.UnitContainer.class);
        if (!unitContainer.units.isEmpty()) {
          return unitContainer.units.get(0).getId();
        }
      }
    }
    catch (IOException e) {
      LOG.error(e.getMessage());
    }
    return -1;
  }

  public static void updateUnit(int unitId, int lessonId, int position, int sectionId, Project project) {
    if (!checkIfAuthorized(project, "updateUnit")) return;

    final HttpPut request = new HttpPut(StepikNames.STEPIK_API_URL + StepikNames.UNITS + "/" + unitId);
    final StepikWrappers.UnitWrapper unitWrapper = new StepikWrappers.UnitWrapper();
    final StepikWrappers.Unit unit = new StepikWrappers.Unit();
    unit.setLesson(lessonId);
    unit.setPosition(position);
    unit.setSection(sectionId);
    unit.setId(unitId);
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
      if (line.getStatusCode() != HttpStatus.SC_OK) {
        LOG.error("Failed to update Unit" + responseString);
        final String detailString = getErrorDetail(responseString);

        showErrorNotification(project, FAILED_TITLE, detailString);
      }
    }
    catch (IOException e) {
      LOG.error(e.getMessage());
    }
  }

  private static int postModule(@NotNull Project project, @NotNull Section section, int courseId) {
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
        final String detailString = getErrorDetail(responseString);

        showErrorNotification(project, FAILED_TITLE, detailString);
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

  public static boolean updateSectionInfo(@NotNull Project project, @NotNull Section section) {
    Section sectionCopy = copySection(section);
    final HttpPut request = new HttpPut(StepikNames.STEPIK_API_URL + StepikNames.SECTIONS + "/" + section.getId());
    final StepikWrappers.SectionWrapper sectionContainer = new StepikWrappers.SectionWrapper();
    sectionContainer.setSection(sectionCopy);
    String requestBody = new Gson().toJson(sectionContainer);
    request.setEntity(new StringEntity(requestBody, ContentType.APPLICATION_JSON));

    try {
      final CloseableHttpClient client = StepikAuthorizedClient.getHttpClient();
      if (client == null) return false;
      final CloseableHttpResponse response = client.execute(request);
      final HttpEntity responseEntity = response.getEntity();
      final String responseString = responseEntity != null ? EntityUtils.toString(responseEntity) : "";
      final StatusLine line = response.getStatusLine();
      EntityUtils.consume(responseEntity);
      if (line.getStatusCode() != HttpStatus.SC_OK) {
        LOG.error(FAILED_TITLE + responseString);
        showErrorNotification(project, FAILED_TITLE, getErrorDetail(responseString));
        return false;
      }
    }
    catch (IOException e) {
      LOG.error(e.getMessage());
      return false;
    }

    return true;
  }

  public static boolean updateTask(@NotNull final Project project, @NotNull final Task task) {
    if (!checkIfAuthorized(project, "update task")) return false;
    final Lesson lesson = task.getLesson();
    final int lessonId = lesson.getId();

    VirtualFile taskDir = task.getTaskDir(project);
    if (taskDir == null) return false;

    final HttpPut request = new HttpPut(StepikNames.STEPIK_API_URL + StepikNames.STEP_SOURCES
                                        + String.valueOf(task.getStepId()));
    final Gson gson = new GsonBuilder().setPrettyPrinting().excludeFieldsWithoutExposeAnnotation().create();

      final Language language = lesson.getCourse().getLanguageById();
      final EduConfigurator configurator = EduConfiguratorManager.forLanguage(language);
      if (configurator == null) return false;
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
    final String[] requestBody = new String[1];
      ApplicationManager.getApplication().invokeAndWait(() -> requestBody[0] = gson.toJson(new StepikWrappers.StepSourceWrapper(project, task, lessonId)));
      request.setEntity(new StringEntity(requestBody[0], ContentType.APPLICATION_JSON));

    try {
      final CloseableHttpClient client = StepikAuthorizedClient.getHttpClient();
      if (client == null) return false;
      final CloseableHttpResponse response = client.execute(request);
      final HttpEntity responseEntity = response.getEntity();
      final String responseString = responseEntity != null ? EntityUtils.toString(responseEntity) : "";
      EntityUtils.consume(responseEntity);
      final StatusLine line = response.getStatusLine();
      switch (line.getStatusCode()) {
        case HttpStatus.SC_OK:
          return true;
        case HttpStatus.SC_NOT_FOUND:
          // TODO: support case when lesson was removed from Stepik too
          return postTask(project, task, task.getLesson().getId());
        default:
          final String message = "Failed to update task ";
          LOG.error(message + responseString);
          return false;
      }
    }
    catch (IOException e) {
      LOG.error(e.getMessage());
    }
    return false;
  }

  public static boolean updateCourseInfo(@NotNull final Project project, @NotNull final RemoteCourse course) {
    if (!checkIfAuthorized(project, "update course")) return false;

    // Course info parameters such as isPublic() and isCompatible can be changed from Stepik site only
    // so we get actual info here
    RemoteCourse courseInfo = getCourseInfo(String.valueOf(course.getId()));
    if (courseInfo != null) {
      course.setPublic(courseInfo.isPublic());
      course.setCompatible(courseInfo.isCompatible());
    }
    else {
      LOG.warn("Failed to get current course info");
    }

    final HttpPut request = new HttpPut(StepikNames.STEPIK_API_URL + StepikNames.COURSES + "/" + String.valueOf(course.getId()));
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

      course.setUpdated();
      return true;
    }
    catch (IOException e) {
      LOG.error(e.getMessage());
      return false;
    }
  }

  public static void updateAdditionalMaterials(@NotNull Project project, int courseId) throws IOException {
    AtomicBoolean additionalMaterialsUpdated = new AtomicBoolean(false);
    RemoteCourse courseInfo = getCourseInfo(String.valueOf(courseId));
    assert courseInfo != null;

    List<Integer> sectionIds = courseInfo.getSectionIds();
    for (Integer sectionId : sectionIds) {
      final Section section = StepikConnector.getSection(sectionId);
      if (StepikNames.PYCHARM_ADDITIONAL.equals(section.getName())) {
        section.setPosition(sectionIds.size());
        section.setId(sectionId);
        updateSectionInfo(project, section);
        final List<Lesson> lessons = StepikConnector.getLessons(courseInfo, sectionId);
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

    RemoteCourse course = (RemoteCourse)StudyTaskManager.getInstance(project).getCourse();
    assert course != null;

    RemoteCourse courseInfo = getCourseInfo(String.valueOf(course.getId()));
    assert courseInfo != null;

    List<Integer> sectionIds = courseInfo.getSectionIds();
    for (Integer sectionId : sectionIds) {
      final Section section = StepikConnector.getSection(sectionId);
      if (StepikNames.PYCHARM_ADDITIONAL.equals(section.getName())) {
        section.setPosition(sectionIds.size());
        updateSectionInfo(project, section);
      }
    }

    return additionalMaterialsUpdated.get();
  }

  public static Lesson updateLessonInfo(@NotNull final Project project, @NotNull final Lesson lesson, boolean showNotification) {
    if (!checkIfAuthorized(project, "update lesson")) return null;

    final HttpPut request = new HttpPut(StepikNames.STEPIK_API_URL + StepikNames.LESSONS + String.valueOf(lesson.getId()));

    String requestBody = new Gson().toJson(new StepikWrappers.LessonWrapper(lesson));
    request.setEntity(new StringEntity(requestBody, ContentType.APPLICATION_JSON));

    try {
      final CloseableHttpClient client = StepikAuthorizedClient.getHttpClient();
      if (client == null) return null;
      final CloseableHttpResponse response = client.execute(request);
      final HttpEntity responseEntity = response.getEntity();
      final String responseString = responseEntity != null ? EntityUtils.toString(responseEntity) : "";
      final StatusLine line = response.getStatusLine();
      EntityUtils.consume(responseEntity);
      // TODO: support case when lesson was removed from Stepik
      if (line.getStatusCode() != HttpStatus.SC_OK) {
        final String message = "Failed to update lesson ";
        LOG.error(message + responseString);
        if (showNotification) {
          final String detailString = getErrorDetail(responseString);

          showErrorNotification(project, message, detailString);
        }
      }

      final Lesson postedLesson = getLessonFromString(responseString);
      if (postedLesson == null) {
        return null;
      }

      int sectionId;
      if (lesson.getSection() == null) {
        RemoteCourse course = (RemoteCourse)StudyTaskManager.getInstance(project).getCourse();
        assert course != null;
        sectionId = course.getSectionIds().get(0);
      }
      else {
        sectionId = lesson.getSection().getId();
      }

      if (!lesson.isAdditional()) {
        updateUnit(lesson.unitId, lesson.getId(), lesson.getIndex(), sectionId, project);
      }

      return new Gson().fromJson(responseString, StepikWrappers.LessonContainer.class).lessons.get(0);
    }
    catch (IOException e) {
      LOG.error(e.getMessage());
    }
    return null;
  }

  public static Lesson updateLesson(@NotNull final Project project,
                                 @NotNull final Lesson lesson,
                                 boolean showNotification) {
    Lesson postedLesson = updateLessonInfo(project, lesson, showNotification);

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
      deleteTask(step, project);
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

  public static void showErrorNotification(@NotNull Project project, String title, String message) {
    final Notification notification =
      new Notification(PUSH_COURSE_GROUP_ID, title, message, NotificationType.ERROR);
    notification.notify(project);
  }

  private static void showNoRightsToUpdateNotification(@NotNull final Project project, @NotNull final RemoteCourse course) {
    String message = "You don't have permission to update the course <br> <a href=\"upload\">Upload to Stepik as New Course</a>";
    Notification notification = new Notification(PUSH_COURSE_GROUP_ID, FAILED_TITLE, message, NotificationType.ERROR,
                                                 new NotificationListener.Adapter() {
                                                   @Override
                                                   protected void hyperlinkActivated(@NotNull Notification notification, @NotNull HyperlinkEvent e) {
                                                     Course nonRemoteCourse =
                                                       XmlSerializer.deserialize(XmlSerializer.serialize(course), Course.class);
                                                     nonRemoteCourse.init(null, null, true);
                                                     StudyTaskManager.getInstance(project).setCourse(nonRemoteCourse);
                                                     postCourseWithProgress(project, nonRemoteCourse);
                                                   }
                                                 });
    notification.notify(project);
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
      public void actionPerformed(AnActionEvent e) {
        BrowserUtil.browse(StepikNames.STEPIK_URL + url);
      }
    };
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
    Course course = StudyTaskManager.getInstance(project).getCourse();
    assert course != null;

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
        final JsonObject details = new JsonParser().parse(responseString).getAsJsonObject();
        final JsonElement detail = details.get("detail");
        final String detailString = detail != null ? detail.getAsString() : responseString;

        showErrorNotification(project, message, detailString);
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

  private static void checkCancelled() {
    final ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
    if (indicator != null) {
      indicator.checkCanceled();
    }
  }

  private static String getErrorDetail(String responseString) {
    final JsonObject details = new JsonParser().parse(responseString).getAsJsonObject();
    final JsonElement detail = details.get("detail");
    return detail != null ? detail.getAsString() : responseString;
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

  public static void deleteSection(@NotNull Project project, final int sectionId) {
    final HttpDelete request = new HttpDelete(StepikNames.STEPIK_API_URL + StepikNames.SECTIONS + "/" + sectionId);
    deleteFromStepik(project, request);
  }

  public static void deleteTask(@NotNull final Integer task, Project project) {
    final HttpDelete request = new HttpDelete(StepikNames.STEPIK_API_URL + StepikNames.STEP_SOURCES + task);
    deleteFromStepik(project, request);
  }

  private static void deleteFromStepik(@NotNull Project project, @NotNull HttpDelete request) {
    try {
      final CloseableHttpClient client = StepikAuthorizedClient.getHttpClient();
      if (client == null) return;
      final CloseableHttpResponse response = client.execute(request);
      final HttpEntity responseEntity = response.getEntity();
      final String responseString = responseEntity != null ? EntityUtils.toString(responseEntity) : "";
      EntityUtils.consume(responseEntity);
      final StatusLine line = response.getStatusLine();
      if (line.getStatusCode() != HttpStatus.SC_NO_CONTENT) {
        LOG.error("Failed to delete item " + responseString);
        final String detailString = getErrorDetail(responseString);

        showErrorNotification(project, "Failed to delete item ", detailString);
      }
    }
    catch (IOException e) {
      LOG.error(e.getMessage());
    }
  }

  public static boolean postTask(final Project project, @NotNull final Task task, final int lessonId) {
    if (!checkIfAuthorized(project, "postTask")) return false;
    if (task instanceof ChoiceTask || task instanceof CodeTask) return false;

    final HttpPost request = new HttpPost(StepikNames.STEPIK_API_URL + "/step-sources");
    final Gson gson = new GsonBuilder().setPrettyPrinting().excludeFieldsWithoutExposeAnnotation().create();
    final String[] requestBody = new String[1];
    ApplicationManager.getApplication().invokeAndWait(() -> requestBody[0] = gson.toJson(new StepikWrappers.StepSourceWrapper(project, task, lessonId)));

    request.setEntity(new StringEntity(requestBody[0], ContentType.APPLICATION_JSON));

    try {
      final CloseableHttpClient client = StepikAuthorizedClient.getHttpClient();
      if (client == null) return false;
      final CloseableHttpResponse response = client.execute(request);
      final StatusLine line = response.getStatusLine();
      final HttpEntity responseEntity = response.getEntity();
      final String responseString = responseEntity != null ? EntityUtils.toString(responseEntity) : "";
      EntityUtils.consume(responseEntity);
      if (line.getStatusCode() != HttpStatus.SC_CREATED) {
        final String message = FAILED_TITLE + "task ";
        LOG.error(message + responseString);
        final String detailString = getErrorDetail(responseString);

        showErrorNotification(project, message, detailString);
        return false;
      }

      final JsonObject postedTask = new Gson().fromJson(responseString, JsonObject.class);
      final JsonObject stepSource = postedTask.getAsJsonArray("step-sources").get(0).getAsJsonObject();
      task.setStepId(stepSource.getAsJsonPrimitive("id").getAsInt());
      return true;
    }
    catch (IOException e) {
      LOG.error(e.getMessage());
    }

    return false;
  }
}
