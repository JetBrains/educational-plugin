package com.jetbrains.edu.coursecreator.stepik;

import com.google.common.annotations.VisibleForTesting;
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
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.xmlb.XmlSerializer;
import com.jetbrains.edu.coursecreator.CCUtils;
import com.jetbrains.edu.learning.EduConfigurator;
import com.jetbrains.edu.learning.EduConfiguratorManager;
import com.jetbrains.edu.learning.EduSettings;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.Lesson;
import com.jetbrains.edu.learning.courseFormat.Section;
import com.jetbrains.edu.learning.*;
import com.jetbrains.edu.learning.courseFormat.*;
import com.jetbrains.edu.learning.courseFormat.ext.CourseExt;
import com.jetbrains.edu.learning.courseFormat.remote.CourseRemoteInfo;
import com.jetbrains.edu.learning.courseFormat.tasks.ChoiceTask;
import com.jetbrains.edu.learning.courseFormat.tasks.CodeTask;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.serialization.SerializationUtils;
import com.jetbrains.edu.learning.stepik.*;
import com.jetbrains.edu.learning.stepik.courseFormat.StepikChangeStatus;
import com.jetbrains.edu.learning.stepik.courseFormat.StepikCourse;
import com.jetbrains.edu.learning.stepik.courseFormat.StepikCourseRemoteInfo;
import com.jetbrains.edu.learning.stepik.courseFormat.StepikSection;
import com.jetbrains.edu.learning.stepik.courseFormat.ext.StepikCourseExt;
import com.jetbrains.edu.learning.stepik.courseFormat.ext.StepikLessonExt;
import com.jetbrains.edu.learning.stepik.courseFormat.ext.StepikSectionExt;
import com.jetbrains.edu.learning.stepik.courseFormat.ext.StepikTaskExt;
import com.jetbrains.edu.learning.stepik.serialization.StepikLessonRemoteInfoAdapter;
import com.jetbrains.edu.learning.stepik.serialization.StepikRemoteInfoAdapter;
import com.jetbrains.edu.learning.stepik.serialization.StepikSectionRemoteInfoAdapter;
import com.jetbrains.edu.learning.stepik.serialization.StepikTaskRemoteInfoAdapter;
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
import java.net.URISyntaxException;
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
  public static StepikCourse getCourseInfo(@NotNull String courseId) {
    final String url = StepikNames.COURSES + "/" + courseId;
    final StepikUser user = EduSettings.getInstance().getUser();
    try {
      final StepikWrappers.CoursesContainer coursesContainer = StepikConnector.getCourseContainers(user, url);
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

  @VisibleForTesting
  public static void postCourse(@NotNull final Project project, @NotNull Course course) {
    if (!checkIfAuthorized(project, "post course")) return;

    final ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
    if (indicator != null) {
      indicator.setText("Uploading course to " + StepikNames.STEPIK_URL);
      indicator.setIndeterminate(false);
    }
    final HttpPost request = new HttpPost(StepikNames.STEPIK_API_URL + StepikNames.COURSES);

    final StepikUserInfo currentUser = StepikAuthorizedClient.getCurrentUser();
    if (currentUser != null) {
      final List<StepikUserInfo> courseAuthors = course.getAuthors();
      for (final StepikUserInfo courseAuthor : courseAuthors) {
        currentUser.setFirstName(courseAuthor.getFirstName());
        currentUser.setLastName(courseAuthor.getLastName());
      }
      course.setAuthors(Collections.singletonList(currentUser));
    }

    String requestBody = getGson(project).toJson(new StepikWrappers.CourseWrapper(course));
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
      final RemoteCourse courseOnRemote = getGson(project).fromJson(responseString, StepikWrappers.CoursesContainer.class).courses.get(0);
      courseOnRemote.setItems(Lists.newArrayList(course.getItems().stream().filter(it -> !it.getName().equals(EduNames.ADDITIONAL_MATERIALS) &&
        !it.getName().equals(StepikNames.PYCHARM_ADDITIONAL)).collect(Collectors.toList())));
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

      postAdditionalFiles(course, project, StepikCourseExt.getId(courseOnRemote), sectionCount + 1);
      courseOnRemote.init(null, null, true);
      StudyTaskManager.getInstance(project).setCourse(courseOnRemote);
      courseOnRemote.init(null, null, true);
      StepikUpdateDateExt.setUpdated(courseOnRemote);
      showNotification(project, "Course is published", openOnStepikAction("/course/" + StepikCourseExt.getId(courseOnRemote)));
    }
    catch (IOException e) {
      LOG.error(e.getMessage());
    }
  }

  private static boolean isTestAccount(@Nullable StepikUserInfo user) {
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

  private static String getAdminsGroupId(@NotNull String responseString) {
    JsonObject coursesObject = new JsonParser().parse(responseString).getAsJsonObject();
    return coursesObject.get("courses").getAsJsonArray().get(0).getAsJsonObject().get("admins_group").getAsString();
  }

  public static void wrapUnpushedLessonsIntoSections(@NotNull Project project, @NotNull Course course) {
    ApplicationManager.getApplication().invokeAndWait(() -> {
      List<Lesson> lessons = course.getLessons();
      for (Lesson lesson : lessons) {
        if (StepikLessonExt.getId(lesson) > 0) {
          continue;
        }
        CCUtils.wrapIntoSection(project, course, Collections.singletonList(lesson), "Section. " + StringUtil.capitalize(lesson.getName()));
      }
    });
  }

  /**
   * This method should be used for courses with sections only
   */
  private static int postSections(@NotNull Project project, @NotNull StepikCourse course) {
    final ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
    course.sortItems();
    final List<Section> sections = course.getSections();
    assert course.getLessons().isEmpty() : "postSections method should be used for courses with sections only";
    int i = 1;
    for (Section item : sections) {
      StepikSection section = new StepikSection();
      StepikSectionExt.setPosition(section, i++);
      section.setName(item.getName());
      List<Lesson> lessons = item.getLessons();

      final int sectionId = postSectionInfo(project, section, StepikCourseExt.getId(course));
      StepikSectionExt.setId(item, sectionId);

      postLessons(project, indicator, course, sectionId, lessons);
    }
    return sections.size();
  }

  private static void postTopLevelLessons(@NotNull Project project, @NotNull StepikCourse course) {
    final ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
    final int sectionId = postSectionForTopLevelLessons(project, course);
    final CourseRemoteInfo info = course.getRemoteInfo();
    assert info instanceof StepikCourseRemoteInfo;
    ((StepikCourseRemoteInfo)info).setSectionIds(Collections.singletonList(sectionId));
    postLessons(project, indicator, course, sectionId, course.getLessons());
  }

  public static int postSectionForTopLevelLessons(@NotNull Project project, @NotNull StepikCourse course) {
    StepikSection section = new StepikSection();
    section.setName(course.getName());
    section.getStepikRemoteInfo().setPosition(1);
    return postSectionInfo(project, section, StepikCourseExt.getId(course));
  }

  public static int findTopLevelLessonsSection(@NotNull StepikCourse course, @Nullable Lesson topLevelLesson) {
    if (topLevelLesson != null) {
      StepikWrappers.Unit unit = StepikConnector.getUnit(StepikLessonExt.getId(topLevelLesson));
      return unit.getSection();
    }
    else {
      StepikCourse courseInfo = StepikConnector
        .getCourseInfo(EduSettings.getInstance().getUser(), StepikCourseExt.getId(course), true);
      if (courseInfo != null) {
        final CourseRemoteInfo info = courseInfo.getRemoteInfo();
        assert info instanceof StepikCourseRemoteInfo;
        String[] sectionIds = ((StepikCourseRemoteInfo)info).getSectionIds().stream().map(s -> String.valueOf(s)).toArray(String[]::new);
        try {
          List<StepikSection> sections = StepikConnector.getSections(sectionIds);
          for (Section section : sections) {
            if (section.getName().equals(courseInfo.getName())) {
              return StepikSectionExt.getId(section);
            }
          }
        }
        catch (URISyntaxException | IOException e) {
          LOG.warn(e.getMessage());
        }
      }
      else {
        LOG.error(String.format("Course with id %s not found on Stepik", StepikCourseExt.getId(course)));
      }
    }

    return -1;
  }

  public static int postSection(@NotNull Project project, @NotNull Section section, @Nullable ProgressIndicator indicator) {
    StepikCourse course = (StepikCourse)StudyTaskManager.getInstance(project).getCourse();
    assert course != null;
    final int sectionId = postSectionInfo(project, copySection(section), StepikCourseExt.getId(course));
    StepikSectionExt.setId(section, sectionId);
    postLessons(project, indicator, course, sectionId, section.getLessons());

    return sectionId;
  }

  public static boolean updateSection(@NotNull Project project, @NotNull Section section) {
    StepikCourse course = (StepikCourse)StudyTaskManager.getInstance(project).getCourse();
    assert course != null;
    StepikSectionExt.setPosition(section, StepikCourseExt.getId(course));
    boolean updated = updateSectionInfo(project, section);
    if (updated) {
      for (Lesson lesson : section.getLessons()) {
        if (StepikLessonExt.getId(lesson) > 0) {
          updateLesson(project, lesson, false, StepikSectionExt.getId(section));
        }
        else {
          postLesson(project, lesson, lesson.getIndex(), StepikSectionExt.getId(section));
        }
      }
    }

    return updated;
  }

  public static Section copySection(@NotNull Section section) {
    Section sectionToPost = new Section();
    sectionToPost.setName(section.getName());
    StepikSectionExt.setPosition(sectionToPost, StepikSectionExt.getPosition(section));
    StepikSectionExt.setId(sectionToPost, StepikSectionExt.getId(section));
    StepikSectionExt.setCourseId(sectionToPost, StepikSectionExt.getCourseId(section));

    return sectionToPost;
  }

  private static void postLessons(@NotNull Project project,
                                  @Nullable ProgressIndicator indicator,
                                  @NotNull StepikCourse course,
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
      showStepikNotification(project, NotificationType.ERROR, failedActionName);
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
      StepikSectionExt.setPosition(section, position);
      final int sectionId = postSectionInfo(project, section, id);
      postLesson(project, lesson, position, sectionId);
    }
  }

  public static void updateAdditionalFiles(@NotNull Course course, @NotNull final Project project, Lesson lesson) {
    final Lesson postedLesson = CCUtils.createAdditionalLesson(course, project, StepikNames.PYCHARM_ADDITIONAL);
    if (postedLesson != null) {
      StepikLessonExt.setId(postedLesson, StepikLessonExt.getId(lesson));
      Section section = lesson.getSection();
      assert section != null;
      postedLesson.setSection(section);
      final ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
      if (indicator != null) {
        indicator.setText2("Publishing additional files");
      }

      Lesson updatedLesson = updateLesson(project, postedLesson, false, StepikSectionExt.getId(section));
      if (updatedLesson != null) {
        final StepikCourseRemoteInfo info = (StepikCourseRemoteInfo)course.getRemoteInfo();
        info.setAdditionalMaterialsUpdateDate(StepikLessonExt.getUpdateDate(updatedLesson));
      }
    }
  }

  public static int postUnit(int lessonId, int position, int sectionId, @NotNull Project project) {
    if (!checkIfAuthorized(project, "postUnit")) return lessonId;

    final HttpPost request = new HttpPost(StepikNames.STEPIK_API_URL + StepikNames.UNITS);
    final StepikWrappers.UnitWrapper unitWrapper = new StepikWrappers.UnitWrapper();
    final StepikWrappers.Unit unit = new StepikWrappers.Unit();
    unit.setLesson(lessonId);
    unit.setPosition(position);
    unit.setSection(sectionId);
    unitWrapper.setUnit(unit);

    String requestBody = getGson(project).toJson(unitWrapper);
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
        StepikWrappers.UnitContainer unitContainer = getGson(project).fromJson(responseString, StepikWrappers.UnitContainer.class);
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

  public static Gson getGson(Project project) {
    final Course course = StudyTaskManager.getInstance(project).getCourse();
    assert course != null;
    final String language = course.getLanguageID();
    return new GsonBuilder()
      .registerTypeAdapter(StepikCourse.class, new StepikRemoteInfoAdapter(language))
      .registerTypeAdapter(Section.class, new StepikSectionRemoteInfoAdapter(language))
      .registerTypeAdapter(Lesson.class, new StepikLessonRemoteInfoAdapter(language))
      .registerTypeAdapter(Task.class, new StepikTaskRemoteInfoAdapter())
      .create();
  }

  public static void updateUnit(int unitId, int lessonId, int position, int sectionId, @NotNull Project project) {
    if (!checkIfAuthorized(project, "updateUnit")) return;

    final HttpPut request = new HttpPut(StepikNames.STEPIK_API_URL + StepikNames.UNITS + "/" + unitId);
    final StepikWrappers.UnitWrapper unitWrapper = new StepikWrappers.UnitWrapper();
    final StepikWrappers.Unit unit = new StepikWrappers.Unit();
    unit.setLesson(lessonId);
    unit.setPosition(position);
    unit.setSection(sectionId);
    unit.setId(unitId);
    unitWrapper.setUnit(unit);

    String requestBody = getGson(project).toJson(unitWrapper);
    request.setEntity(new StringEntity(requestBody, ContentType.APPLICATION_JSON));

    try {
      final CloseableHttpClient client = StepikAuthorizedClient.getHttpClient();
      if (client == null) return;
      final CloseableHttpResponse response = client.execute(request);
      final HttpEntity responseEntity = response.getEntity();
      final String responseString = responseEntity != null ? EntityUtils.toString(responseEntity) : "";
      final StatusLine line = response.getStatusLine();
      EntityUtils.consume(responseEntity);
      if (line.getStatusCode() != HttpStatus.SC_OK && line.getStatusCode() != HttpStatus.SC_CREATED) {
        LOG.error("Failed to update Unit" + responseString);
        final String detailString = getErrorDetail(responseString);

        showErrorNotification(project, FAILED_TITLE, detailString);
      }
    }
    catch (IOException e) {
      LOG.error(e.getMessage());
    }
  }

  public static int postSectionInfo(@NotNull Project project, @NotNull Section section, int courseId) {
    final HttpPost request = new HttpPost(StepikNames.STEPIK_API_URL + StepikNames.SECTIONS);
    StepikSectionExt.setCourseId(section, courseId);
    final StepikWrappers.SectionWrapper sectionContainer = new StepikWrappers.SectionWrapper();
    sectionContainer.setSection(section);
    String requestBody = getGson(project).toJson(sectionContainer);
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
      final Section postedSection = getGson(project).fromJson(responseString, StepikWrappers.SectionContainer.class).getSections().get(0);
      StepikSectionExt.setId(section, StepikSectionExt.getId(postedSection));
      return StepikSectionExt.getId(postedSection);
    }
    catch (IOException e) {
      LOG.error(e.getMessage());
    }
    return -1;
  }

  public static boolean updateSectionInfo(@NotNull Project project, @NotNull Section section) {
    Section sectionCopy = copySection(section);
    final HttpPut request = new HttpPut(StepikNames.STEPIK_API_URL + StepikNames.SECTIONS + "/" + StepikSectionExt.getId(section));
    final StepikWrappers.SectionWrapper sectionContainer = new StepikWrappers.SectionWrapper();
    sectionContainer.setSection(sectionCopy);
    String requestBody = getGson(project).toJson(sectionContainer);
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
    final int lessonId = StepikLessonExt.getId(lesson);

    VirtualFile taskDir = task.getTaskDir(project);
    if (taskDir == null) return false;

    final HttpPut request = new HttpPut(StepikNames.STEPIK_API_URL + StepikNames.STEP_SOURCES
                                        + String.valueOf(StepikTaskExt.getStepId(task)));
    final Gson gson = new GsonBuilder().setPrettyPrinting().excludeFieldsWithoutExposeAnnotation().create();

    final Language language = lesson.getCourse().getLanguageById();
    final EduConfigurator configurator = EduConfiguratorManager.forLanguage(language);
    if (configurator == null) return false;
    final String[] requestBody = new String[1];
    ApplicationManager.getApplication().invokeAndWait(() -> {
      FileDocumentManager.getInstance().saveAllDocuments();
      requestBody[0] = gson.toJson(new StepikWrappers.StepSourceWrapper(project, task, lessonId));
    });
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
          return postTask(project, task, StepikLessonExt.getId(task.getLesson()));
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

  public static boolean updateCourseInfo(@NotNull final Project project, @NotNull final StepikCourse course) {
    if (!checkIfAuthorized(project, "update course")) return false;

    // Course info parameters such as is_public and is_idea_compatible can be changed from Stepik site only
    // so we get actual info here
    StepikCourse courseInfo = getCourseInfo(String.valueOf(StepikCourseExt.getId(course)));
    if (courseInfo != null) {
      final CourseRemoteInfo localRemoteInfo = course.getRemoteInfo();
      final CourseRemoteInfo remoteInfo = courseInfo.getRemoteInfo();
      if (localRemoteInfo instanceof StepikCourseRemoteInfo && remoteInfo instanceof StepikCourseRemoteInfo) {
        ((StepikCourseRemoteInfo)localRemoteInfo).setPublic(((StepikCourseRemoteInfo)remoteInfo).isPublic());
        ((StepikCourseRemoteInfo)localRemoteInfo).setIdeaCompatible(((StepikCourseRemoteInfo)remoteInfo).isIdeaCompatible());
      }
    }
    else {
      LOG.warn("Failed to get current course info");
    }

    final HttpPut request = new HttpPut(StepikNames.STEPIK_API_URL + StepikNames.COURSES + "/" + String.valueOf(StepikCourseExt.getId(course)));
    String requestBody = getGson(project).toJson(new StepikWrappers.CourseWrapper(course));
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

  public static void updateAdditionalMaterials(@NotNull Project project, int courseId) throws IOException {
    AtomicBoolean additionalMaterialsUpdated = new AtomicBoolean(false);
    StepikCourse courseInfo = getCourseInfo(String.valueOf(courseId));
    assert courseInfo != null;

    final CourseRemoteInfo remoteInfo = courseInfo.getRemoteInfo();
    assert remoteInfo instanceof StepikCourseRemoteInfo;
    List<Integer> sectionIds = ((StepikCourseRemoteInfo)remoteInfo).getSectionIds();
    for (Integer sectionId : sectionIds) {
      final Section section = StepikConnector.getSection(sectionId);
      if (StepikNames.PYCHARM_ADDITIONAL.equals(section.getName())) {
        StepikSectionExt.setPosition(section, sectionIds.size());
        StepikSectionExt.setId(section, sectionId);
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

    StepikCourse course = (StepikCourse)StudyTaskManager.getInstance(project).getCourse();
    assert course != null;

    StepikCourse courseInfo = getCourseInfo(String.valueOf(StepikCourseExt.getId(course)));
    assert courseInfo != null;

    final CourseRemoteInfo remoteInfo = courseInfo.getRemoteInfo();
    assert remoteInfo instanceof StepikCourseRemoteInfo;
    List<Integer> sectionIds = ((StepikCourseRemoteInfo)remoteInfo).getSectionIds();
    for (Integer sectionId : sectionIds) {
      final Section section = StepikConnector.getSection(sectionId);
      if (StepikNames.PYCHARM_ADDITIONAL.equals(section.getName())) {
        StepikSectionExt.setPosition(section, sectionIds.size());
        updateSectionInfo(project, section);
      }
    }

    return additionalMaterialsUpdated.get();
  }

  public static Lesson updateLessonInfo(@NotNull final Project project,
                                        @NotNull final Lesson lesson,
                                        boolean showNotification, int sectionId) {
    if (!checkIfAuthorized(project, "update lesson")) return null;

    final HttpPut request = new HttpPut(StepikNames.STEPIK_API_URL + StepikNames.LESSONS + String.valueOf(StepikLessonExt.getId(lesson)));

    String requestBody = getGson(project).toJson(new StepikWrappers.LessonWrapper(lesson));
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

      final Lesson postedLesson = getLessonFromString(responseString, project);
      if (postedLesson == null) {
        return null;
      }

      if (!lesson.isAdditional()) {
        updateUnit(StepikLessonExt.getUnitId(lesson), StepikLessonExt.getId(lesson), lesson.getIndex(), sectionId, project);
      }

      return getGson(project).fromJson(responseString, StepikWrappers.LessonContainer.class).lessons.get(0);
    }
    catch (IOException e) {
      LOG.error(e.getMessage());
    }
    return null;
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
      .map(task -> StepikTaskExt.getStepId(task))
      .filter(id -> id > 0)
      .collect(Collectors.toSet());

    final List<Integer> taskIdsToDelete = StepikLessonExt.getSteps(remoteLesson).stream()
      .filter(id -> !localTasksIds.contains(id))
      .collect(Collectors.toList());

    // Remove all tasks from Stepik which are not in our lessons now
    for (Integer step : taskIdsToDelete) {
      deleteTask(step);
    }

    for (Task task : localLesson.getTaskList()) {
      checkCancelled();
      if (StepikTaskExt.getStepId(task) > 0) {
        updateTask(project, task);
      }
      else {
        postTask(project, task, StepikLessonExt.getId(localLesson));
      }
    }
  }

  public static void showErrorNotification(@NotNull Project project, @NotNull String title, @NotNull String message) {
    final Notification notification =
      new Notification(PUSH_COURSE_GROUP_ID, title, message, NotificationType.ERROR);
    notification.notify(project);
  }

  private static void showNoRightsToUpdateNotification(@NotNull final Project project, @NotNull final StepikCourse course) {
    String message = "You don't have permission to update the course <br> <a href=\"upload\">Upload to Stepik as New Course</a>";
    Notification notification = new Notification(PUSH_COURSE_GROUP_ID, FAILED_TITLE, message, NotificationType.ERROR,
                                                 createPostCourseNotificationListener(project, course));
    notification.notify(project);
  }

  @NotNull
  public static NotificationListener.Adapter createPostCourseNotificationListener(@NotNull Project project,
                                                                                   @NotNull RemoteCourse course) {
    return new NotificationListener.Adapter() {
      @Override
      protected void hyperlinkActivated(@NotNull Notification notification, @NotNull HyperlinkEvent e) {
        notification.expire();
        Course nonRemoteCourse =
          XmlSerializer.deserialize(XmlSerializer.serialize(course), Course.class);
        nonRemoteCourse.init(null, null, true);
        StudyTaskManager.getInstance(project).setCourse(nonRemoteCourse);
        postCourseWithProgress(project, nonRemoteCourse);
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
      public void actionPerformed(AnActionEvent e) {
        BrowserUtil.browse(StepikNames.STEPIK_URL + url);
      }
    };
  }

  private static void showStepikNotification(@NotNull Project project,
                                             @NotNull NotificationType notificationType, @NotNull String failedActionName) {
    String text = "Log in to Stepik to " + failedActionName;
    Notification notification = new Notification("Stepik", "Failed to " + failedActionName, text, notificationType);
    notification.addAction(new DumbAwareAction("Log in") {

      @Override
      public void actionPerformed(AnActionEvent e) {
        StepikConnector.doAuthorize(() -> showOAuthDialog());
        notification.expire();
      }
    });

    notification.notify(project);
  }

  public static int postLesson(@NotNull final Project project, @NotNull final Lesson lesson, int position, int sectionId) {
    Lesson postedLesson = postLessonInfo(project, lesson, sectionId, position);

    if (postedLesson == null) {
      return -1;
    }
    StepikLessonExt.setId(lesson, StepikLessonExt.getId(postedLesson));
    StepikLessonExt.setUnitId(lesson, StepikLessonExt.getUnitId(postedLesson));
    for (Task task : lesson.getTaskList()) {
      final ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
      if (indicator != null) {
        indicator.checkCanceled();
      }
      postTask(project, task, StepikLessonExt.getId(postedLesson));
    }

    return StepikLessonExt.getId(postedLesson);
  }

  public static Lesson postLessonInfo(@NotNull Project project, @NotNull Lesson lesson, int sectionId, int position) {
    if (!checkIfAuthorized(project, "postLesson")) return null;
    Course course = StudyTaskManager.getInstance(project).getCourse();
    assert course != null;

    final HttpPost request = new HttpPost(StepikNames.STEPIK_API_URL + "/lessons");

    String requestBody = getGson(project).toJson(new StepikWrappers.LessonWrapper(lesson));
    request.setEntity(new StringEntity(requestBody, ContentType.APPLICATION_JSON));

    Lesson postedLesson = null;
    try {
      final CloseableHttpClient client = StepikAuthorizedClient.getHttpClient();
      if (client == null) return null;
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
        return null;
      }

      postedLesson = getLessonFromString(responseString, project);
      if (postedLesson != null) {
        StepikLessonExt.setUnitId(postedLesson, postUnit(StepikLessonExt.getId(postedLesson), position, sectionId, project));
      }
    }
    catch (IOException e) {
      LOG.error(e.getMessage());
    }
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

  @Nullable
  public static Lesson getLessonFromString(@NotNull String responseString, Project project) {
    final JsonObject jsonTree = getGson(project).fromJson(responseString, JsonObject.class);
    if (jsonTree.has(SerializationUtils.LESSONS)) {
      final JsonArray lessons = jsonTree.get(SerializationUtils.LESSONS).getAsJsonArray();
      if (lessons.size() == 1) {
        return getGson(project).fromJson(lessons.get(0), Lesson.class);
      }
    }
    return null;
  }

  public static void deleteSection(final int sectionId) {
    final HttpDelete request = new HttpDelete(StepikNames.STEPIK_API_URL + StepikNames.SECTIONS + "/" + sectionId);
    deleteFromStepik(request);
  }

  public static void deleteLesson(final int lessonId) {
    final HttpDelete request = new HttpDelete(StepikNames.STEPIK_API_URL + StepikNames.LESSONS + "/" + lessonId);
    deleteFromStepik(request);
  }

  public static void deleteUnit(final int unitId) {
    final HttpDelete request = new HttpDelete(StepikNames.STEPIK_API_URL + StepikNames.UNITS + "/" + unitId);
    deleteFromStepik(request);
  }

  public static void deleteTask(int task) {
    final HttpDelete request = new HttpDelete(StepikNames.STEPIK_API_URL + StepikNames.STEP_SOURCES + task);
    deleteFromStepik(request);
  }

  private static void deleteFromStepik(@NotNull HttpDelete request) {
    try {
      final CloseableHttpClient client = StepikAuthorizedClient.getHttpClient();
      if (client == null) return;
      final CloseableHttpResponse response = client.execute(request);
      final HttpEntity responseEntity = response.getEntity();
      final String responseString = responseEntity != null ? EntityUtils.toString(responseEntity) : "";
      EntityUtils.consume(responseEntity);
      final StatusLine line = response.getStatusLine();
      if (line.getStatusCode() != HttpStatus.SC_NO_CONTENT) {
        // If parent item was deleted its children are deleted too, so
        // it's ok to fail to delete item here
        LOG.warn("Failed to delete item " + responseString);
      }
    }
    catch (IOException e) {
      LOG.error(e.getMessage());
    }
  }

  public static boolean postTask(@NotNull final Project project, @NotNull final Task task, final int lessonId) {
    if (!checkIfAuthorized(project, "postTask")) return false;
    if (task instanceof ChoiceTask || task instanceof CodeTask) return false;

    final HttpPost request = new HttpPost(StepikNames.STEPIK_API_URL + "/step-sources");
    final Gson gson = new GsonBuilder().setPrettyPrinting().excludeFieldsWithoutExposeAnnotation().create();
    final String[] requestBody = new String[1];
    ApplicationManager.getApplication().invokeAndWait(
      () -> ApplicationManager.getApplication().runWriteAction(() -> {
        FileDocumentManager.getInstance().saveAllDocuments();
        requestBody[0] = gson.toJson(new StepikWrappers.StepSourceWrapper(project, task, lessonId));
      }));

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
      StepikTaskExt.setStepId(task, stepSource.getAsJsonPrimitive("id").getAsInt());
      return true;
    }
    catch (IOException e) {
      LOG.error(e.getMessage());
    }

    return false;
  }

  public static int getTopLevelSectionId(@NotNull Project project, @NotNull StepikCourse course) {
    final CourseRemoteInfo info = course.getRemoteInfo();
    assert info instanceof StepikCourseRemoteInfo;
    final List<Integer> sectionIds = ((StepikCourseRemoteInfo)info).getSectionIds();
    if (!sectionIds.isEmpty()) {
      return sectionIds.get(0);
    }
    else {
      Lesson topLevelLesson = getTopLevelLesson(course);
      if (topLevelLesson == null) {
        LOG.warn("Failed to find top-level lesson for a course: " + StepikCourseExt.getId(course));
        return -1;
      }

      int id = findTopLevelLessonsSection(course, topLevelLesson);
      if (id != -1) {
        return id;
      }
      else {
        return postSectionForTopLevelLessons(project, course);
      }
    }
  }

  @Nullable
  private static Lesson getTopLevelLesson(StepikCourse course) {
    for (Lesson lesson : course.getLessons()) {
      if (lesson.getStepikChangeStatus() == StepikChangeStatus.UP_TO_DATE) {
        return lesson;
      }
    }

    return null;
  }
}
