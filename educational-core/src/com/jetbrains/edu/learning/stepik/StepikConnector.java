package com.jetbrains.edu.learning.stepik;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.intellij.ide.BrowserUtil;
import com.intellij.lang.Language;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task.Backgroundable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ConcurrencyUtil;
import com.intellij.util.containers.ContainerUtil;
import com.jetbrains.edu.learning.EduNames;
import com.jetbrains.edu.learning.EduSettings;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.authUtils.CustomAuthorizationServer;
import com.jetbrains.edu.learning.courseFormat.*;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.builtInWebServer.BuiltInServerOptions;
import org.jetbrains.ide.BuiltInServerManager;

import javax.swing.event.HyperlinkEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.jetbrains.edu.learning.stepik.StepikWrappers.*;

public class StepikConnector {
  private static final Logger LOG = Logger.getInstance(StepikConnector.class.getName());

  private static final String OPEN_PLACEHOLDER_TAG = "<placeholder>";
  private static final String CLOSE_PLACEHOLDER_TAG = "</placeholder>";
  private static final String PROMOTED_COURSES_LINK = "https://raw.githubusercontent.com/JetBrains/educational-plugin/master/featured_courses.txt";
  private static final String IN_PROGRESS_COURSES_LINK = "https://raw.githubusercontent.com/JetBrains/educational-plugin/master/in_progress_courses.txt";
  public static final int MAX_REQUEST_PARAMS = 100; // restriction of Stepik API for multiple requests
  private static final int THREAD_NUMBER = Runtime.getRuntime().availableProcessors();
  private static final ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(THREAD_NUMBER);

  public static final Key<String> COURSE_LANGUAGE = Key.create("COURSE_LANGUAGE");
  private static final ExclusionStrategy ourExclusionStrategy = new ExclusionStrategy() {
    @Override
    public boolean shouldSkipField(FieldAttributes f) {
      return false;
    }

    @Override
    public boolean shouldSkipClass(Class<?> clazz) {
      return clazz == AttemptWrapper.Dataset.class;
    }
  };

  public static final List<Integer> FEATURED_COURSES = getFeaturedCoursesIds();
  public static final List<Integer> IN_PROGRESS_COURSES = getInProgressCoursesIds();

  private StepikConnector() {
  }

  public static void enrollToCourse(final int courseId, @Nullable final StepikUser user) {
    if (user == null) return;
    HttpPost post = new HttpPost(StepikNames.STEPIK_API_URL + StepikNames.ENROLLMENTS);
    try {
      final EnrollmentWrapper enrollment = new EnrollmentWrapper(String.valueOf(courseId));
      post.setEntity(new StringEntity(new GsonBuilder().create().toJson(enrollment)));
      final CloseableHttpClient client = StepikAuthorizedClient.getHttpClient(user);
      CloseableHttpResponse response = client.execute(post);
      StatusLine line = response.getStatusLine();
      line.getStatusCode();
    }
    catch (IOException e) {
      LOG.warn(e.getMessage());
    }
  }

  public static boolean isEnrolledToCourse(final int courseId, @Nullable final StepikUser user) {
    if (user == null) return false;
    HttpGet request = new HttpGet(StepikNames.STEPIK_API_URL + StepikNames.ENROLLMENTS + "/" + courseId);
    try {
      final CloseableHttpClient client = StepikAuthorizedClient.getHttpClient(user);
      CloseableHttpResponse response = client.execute(request);
      StatusLine line = response.getStatusLine();
      return line.getStatusCode() == HttpStatus.SC_OK;
    }
    catch (IOException e) {
      LOG.warn(e.getMessage());
    }
    return false;
  }

  @NotNull
  public static List<RemoteCourse> getCourseInfos(@Nullable StepikUser user) {
    LOG.info("Loading courses started...");
    long startTime = System.currentTimeMillis();
    List<RemoteCourse> result = ContainerUtil.newArrayList();
    List<Callable<List<RemoteCourse>>> tasks = ContainerUtil.newArrayList();
    tasks.add(() -> getCourseInfos(user, true));
    tasks.add(() -> getCourseInfos(user, false));
    tasks.add(() -> getInProgressCourses(user));

    try {
      for (Future<List<RemoteCourse>> future : ConcurrencyUtil.invokeAll(tasks, EXECUTOR_SERVICE)) {
        if (!future.isCancelled()) {
          List<RemoteCourse> courses = future.get();
          if (courses != null) {
            result.addAll(courses);
          }
        }
      }
    }
    catch (Throwable e) {
      LOG.warn("Cannot load course list " + e.getMessage());
    }
    setAuthors(result);

    LOG.info("Loading courses finished...Took " + (System.currentTimeMillis() - startTime) + " ms");
    return result;
  }

  private static List<RemoteCourse> getCourseInfos(@Nullable StepikUser user, boolean isPublic) {
    List<RemoteCourse> result = ContainerUtil.newArrayList();
    try {
      int pageNumber = 1;
      final ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
      while (addCourseInfos(user, result, getParameters(pageNumber, isPublic))) {
        if (indicator != null && indicator.isCanceled()) {
          break;
        }
        pageNumber += 1;
      }
    }
    catch (IOException e) {
      LOG.warn("Cannot load course list " + e.getMessage());
    }
    return result;
  }

  private static List<NameValuePair> getParameters(int pageNumber, boolean isPublic) {
    final ArrayList<NameValuePair> parameters = ContainerUtil.newArrayList(new BasicNameValuePair("is_idea_compatible", "true"),
                                                                      new BasicNameValuePair("is_public", String.valueOf(isPublic)),
                                                                      new BasicNameValuePair("page", String.valueOf(pageNumber)));
    if (!isPublic) {
      parameters.add(new BasicNameValuePair("enrolled", "true"));
    }
    return parameters;
  }

  private static void setAuthors(List<RemoteCourse> result) {
    final Set<Integer> allInstructors = result.stream().map(it -> it.getInstructors()).flatMap(List::stream).collect(Collectors.toSet());
    final String[] instructorIds = allInstructors.stream().map(it -> String.valueOf(it)).toArray(String[]::new);
    try {
      final List<AuthorWrapper> authors = multipleRequestToStepik(StepikNames.USERS, instructorIds, AuthorWrapper.class);
      final Map<Integer, StepikUserInfo> infoMap =
        authors.stream().flatMap(it -> it.users.stream()).collect(Collectors.toMap(userInfo -> userInfo.getId(), userInfo -> userInfo));
      for (RemoteCourse course : result) {
        List<StepikUserInfo> courseAuthors = course.getInstructors().stream().map(infoMap::get).collect(Collectors.toList());
        course.setAuthors(courseAuthors);
      }
    }
    catch (IOException e) {
      LOG.warn("Cannot load course list " + e.getMessage());
    }
  }

  private static List<RemoteCourse> getInProgressCourses(@Nullable StepikUser user) {
    List<RemoteCourse> result = ContainerUtil.newArrayList();
    for (Integer courseId : IN_PROGRESS_COURSES) {
      final RemoteCourse info = getCourseInfo(user, courseId, false);
      if (info == null) continue;
      CourseCompatibility compatibility = info.getCompatibility();
      if (compatibility == CourseCompatibility.UNSUPPORTED) continue;
      CourseVisibility visibility = new CourseVisibility.InProgressVisibility(IN_PROGRESS_COURSES.indexOf(info.getId()));
      info.setVisibility(visibility);
      result.add(info);
    }
    return result;
  }

  public static void updateCourseIfNeeded(@NotNull Project project, @NotNull RemoteCourse course) {
    int id = course.getId();

    if (id == 0) {
      return;
    }

    if (!course.isStudy()) {
      return;
    }

    ProgressManager.getInstance().runProcessWithProgressAsynchronously(new Backgroundable(project, "Updating Course") {
      @Override
      public void run(@NotNull ProgressIndicator indicator) {
        if (!StepikUpdateDateExt.isUpToDate(course)) {
          showUpdateAvailableNotification(project, course);
        }
      }
    }, new EmptyProgressIndicator());
  }

  private static void showUpdateAvailableNotification(@NotNull Project project, @NotNull Course course) {
    final Notification notification =
      new Notification("Update.course", "Course Updates", "Course is ready to <a href=\"update\">update</a>", NotificationType.INFORMATION,
                       new NotificationListener() {
                         @Override
                         public void hyperlinkUpdate(@NotNull Notification notification, @NotNull HyperlinkEvent event) {
                           FileEditorManagerEx.getInstanceEx(project).closeAllFiles();

                           ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
                             ProgressManager.getInstance().getProgressIndicator().setIndeterminate(true);
                             new StepikCourseUpdater((RemoteCourse)course, project).updateCourse();
                             StepikUpdateDateExt.setUpdated((RemoteCourse)course);
                           }, "Updating Course", true, project);
                         }
                       });
    notification.notify(project);
  }

  public static int getTaskPosition(final int taskId) {
    final String url = StepikNames.STEPS + taskId;
    try {
      StepContainer container = StepikAuthorizedClient.getFromStepik(url, StepContainer.class);
      if (container == null) {
        container = StepikClient.getFromStepik(url, StepContainer.class);
      }
      List<StepSource> steps = container.steps;
      if (!steps.isEmpty()) {
        return steps.get(0).position;
      }
    }
    catch (IOException e) {
      LOG.warn("Could not retrieve task with id=" + taskId);
    }

    return -1;
  }

  private static CoursesContainer getCourseContainers(@Nullable StepikUser user, @NotNull URI url) throws IOException {
    return getCourseContainers(user, url.toString());
  }

  public static CoursesContainer getCourseContainers(@Nullable StepikUser user, @NotNull String url) throws IOException {
    final CoursesContainer coursesContainer;
    if (user != null) {
      coursesContainer = StepikAuthorizedClient.getFromStepik(url, CoursesContainer.class, user);
    }
    else {
      coursesContainer = StepikClient.getFromStepik(url, CoursesContainer.class);
    }
    if (coursesContainer != null) {
      for (RemoteCourse info : coursesContainer.courses) {
        StepikUtils.setCourseLanguage(info);
      }
    }
    return coursesContainer;
  }

  private static boolean addCourseInfos(@Nullable StepikUser user, List<RemoteCourse> result,
                                        @NotNull List<NameValuePair> parameters) throws IOException {
    final URI url;
    try {
      url = new URIBuilder(StepikNames.COURSES).addParameters(parameters).build();
    }
    catch (URISyntaxException e) {
      LOG.error(e.getMessage());
      return false;
    }
    final CoursesContainer coursesContainer = getCourseContainers(user, url);
    addAvailableCourses(result, coursesContainer);
    return coursesContainer.meta.containsKey("has_next") && coursesContainer.meta.get("has_next") == Boolean.TRUE;
  }

  @Nullable
  public static RemoteCourse getCourseInfo(@Nullable StepikUser user, int courseId, boolean isIdeaCompatible) {
    final URI url;
    final CoursesContainer coursesContainer;
    try {
      url = new URIBuilder(StepikNames.COURSES + "/" + courseId)
        .addParameter("is_idea_compatible", String.valueOf(isIdeaCompatible))
        .build();
      coursesContainer = getCourseContainers(user, url);
    }
    catch (URISyntaxException | IOException e) {
      LOG.warn(e.getMessage());
      return null;
    }

    if (coursesContainer != null && !coursesContainer.courses.isEmpty()) {
      return coursesContainer.courses.get(0);
    } else {
      return null;
    }
  }

  private static void addAvailableCourses(List<RemoteCourse> result, CoursesContainer coursesContainer) {
    final List<RemoteCourse> courses = coursesContainer.courses;
    for (RemoteCourse info : courses) {
      if (StringUtil.isEmptyOrSpaces(info.getType())) continue;

      CourseCompatibility compatibility = info.getCompatibility();
      if (compatibility == CourseCompatibility.UNSUPPORTED) continue;

      info.setVisibility(getVisibility(info));
      result.add(info);
    }
  }

  private static CourseVisibility getVisibility(@NotNull RemoteCourse course) {
    if (!course.isPublic()) {
      return CourseVisibility.PrivateVisibility.INSTANCE;
    }
    if (FEATURED_COURSES.contains(course.getId())) {
      return new CourseVisibility.FeaturedVisibility(FEATURED_COURSES.indexOf(course.getId()));
    }
    if (FEATURED_COURSES.isEmpty()) {
      return CourseVisibility.LocalVisibility.INSTANCE;
    }
    return CourseVisibility.PublicVisibility.INSTANCE;
  }

  public static boolean loadCourseStructure(@NotNull final RemoteCourse remoteCourse) {
    final List<StudyItem> items = remoteCourse.getItems();
    if (!items.isEmpty()) return true;
    try {
      fillItems(remoteCourse);
      return true;
    }
    catch (IOException e) {
      LOG.error(e);
      return false;
    }
  }

  public static void fillItems(@NotNull RemoteCourse remoteCourse) throws IOException {
    String[] sectionIds = remoteCourse.getSectionIds().stream().map(section -> String.valueOf(section)).toArray(String[]::new);
    List<Section> allSections = getSections(sectionIds);

    if (hasVisibleSections(allSections, remoteCourse.getName())) {
      remoteCourse.setSectionIds(Collections.emptyList());
      List<Callable<StudyItem>> tasks = ContainerUtil.newArrayList();
      for (int index = 0; index < allSections.size(); index++) {
        Section section = allSections.get(index);
        int finalIndex = index + 1;
        tasks.add(() -> loadItemTask(remoteCourse, section, finalIndex));
      }
      remoteCourse.setItems(getAllItems(remoteCourse, tasks));
    }
    else {
      addTopLevelLessons(remoteCourse, allSections);
    }
  }

  private static List<StudyItem> getAllItems(@NotNull RemoteCourse remoteCourse, List<Callable<StudyItem>> tasks) {
    try {
      List<StudyItem> sections = getOrderedListOfSections(tasks);
      ArrayList<StudyItem> items = unpackTopLevelLessons(remoteCourse, sections);
      setIndices(items);
      return items;
    }
    catch (Throwable e) {
      LOG.warn("Cannot load sections for course " + remoteCourse.getId() + e.getMessage());
    }

    return Collections.emptyList();
  }

  private static List<StudyItem> getOrderedListOfSections(List<Callable<StudyItem>> tasks) throws Throwable {
    List<StudyItem> sections = new ArrayList<>();
    for (Future<StudyItem> future : ConcurrencyUtil.invokeAll(tasks, EXECUTOR_SERVICE)) {
      if (!future.isCancelled()) {
        final StudyItem item = future.get();
        if (item != null) {
          sections.add(item.getIndex() - 1, item);
        }
      }
    }

    return sections;
  }

  private static void setIndices(ArrayList<StudyItem> items) {
    for (int i = 0; i < items.size(); i++) {
      StudyItem item = items.get(i);
      item.setIndex(i + 1);
    }
  }

  private static ArrayList<StudyItem> unpackTopLevelLessons(@NotNull RemoteCourse remoteCourse, List<StudyItem> sections) {
    ArrayList<StudyItem> itemsWithTopLevelLessons = new ArrayList<>();
    for (StudyItem item : sections) {
      if (item instanceof Section && item.getName().equals(remoteCourse.getName())) {
        remoteCourse.setSectionIds(Collections.singletonList(item.getId()));
        itemsWithTopLevelLessons.addAll(((Section)item).getLessons());
      }
      else {
        itemsWithTopLevelLessons.add(item);
      }
    }
    return itemsWithTopLevelLessons;
  }

  private static void addTopLevelLessons(@NotNull RemoteCourse remoteCourse, List<Section> allSections)
    throws IOException {
    final String[] unitIds = allSections.stream().map(section -> section.units).flatMap(unitList -> unitList.stream())
      .map(unit -> String.valueOf(unit)).toArray(String[]::new);
    if (unitIds.length > 0) {
      final List<Lesson> lessons = getLessons(remoteCourse);
      remoteCourse.addLessons(lessons);
      remoteCourse.setSectionIds(allSections.stream().map(s -> s.getId()).collect(Collectors.toList()));
      lessons.stream().filter(lesson -> lesson.isAdditional()).forEach(lesson -> remoteCourse.setAdditionalMaterialsUpdateDate(lesson.getUpdateDate()));
    }
  }

  @Nullable
  private static StudyItem loadItemTask(@NotNull RemoteCourse remoteCourse, Section section, int finalIndex)
    throws IOException {
    final String[] unitIds = section.units.stream().map(unit -> String.valueOf(unit)).toArray(String[]::new);
    if (unitIds.length <= 0) {
      return null;
    }
    final List<Lesson> lessonsFromUnits = getLessonsFromUnits(remoteCourse, unitIds, false);
    final String sectionName = section.getName();
    if (sectionName.equals(StepikNames.PYCHARM_ADDITIONAL)) {
      final Lesson lesson = lessonsFromUnits.get(0);
      lesson.setIndex(finalIndex);
      remoteCourse.setAdditionalMaterialsUpdateDate(lesson.getUpdateDate());
      return lesson;
    }
    else {
      for (int i = 0; i < lessonsFromUnits.size(); i++) {
        Lesson lesson = lessonsFromUnits.get(i);
        lesson.setIndex(i + 1);
      }
      section.addLessons(lessonsFromUnits);
      section.setIndex(finalIndex);
      return section;
    }
  }

  public static List<Section> getSections(String[] sectionIds) throws IOException {
    List<SectionContainer> containers = multipleRequestToStepik(StepikNames.SECTIONS, sectionIds, SectionContainer.class);
    return containers.stream().map(container -> container.sections).flatMap(sections -> sections.stream())
      .collect(Collectors.toList());
  }

  @NotNull
  public static List<Unit> getUnits(String[] unitIds) throws IOException {
    List<UnitContainer> unitContainers = multipleRequestToStepik(StepikNames.UNITS, unitIds, UnitContainer.class);
    return unitContainers.stream().flatMap(container -> container.units.stream()).collect(Collectors.toList());
  }

  private static List<Lesson> getLessons(RemoteCourse remoteCourse) throws IOException {
    String[] unitIds = getUnitsIds(remoteCourse);
    if (unitIds.length > 0) {
      return getLessonsFromUnits(remoteCourse, unitIds, true);
    }

    return Collections.emptyList();
  }

  public static List<Lesson> getLessons(RemoteCourse remoteCourse, int sectionId) throws IOException {
    final SectionContainer sectionContainer = getFromStepik(StepikNames.SECTIONS + "/" + sectionId,
            SectionContainer.class);
    if (sectionContainer.sections.isEmpty()) {
      return Collections.emptyList();
    }
    Section firstSection = sectionContainer.sections.get(0);
    String[] unitIds = firstSection.units.stream().map(id -> String.valueOf(id)).toArray(String[]::new);

    return new ArrayList<>(getLessonsFromUnits(remoteCourse, unitIds, true));
  }

  public static String[] getUnitsIds(RemoteCourse remoteCourse) throws IOException {
    String[] sectionIds = remoteCourse.getSectionIds().stream().map(section -> String.valueOf(section)).toArray(String[]::new);
    List<SectionContainer> containers = multipleRequestToStepik(StepikNames.SECTIONS, sectionIds, SectionContainer.class);
    Stream<Section> allSections = containers.stream().map(container -> container.sections).flatMap(sections -> sections.stream());
    return allSections
      .map(section -> section.units)
      .flatMap(unitList -> unitList.stream())
      .map(unit -> String.valueOf(unit))
      .toArray(String[]::new);
  }

  private static boolean hasVisibleSections(@NotNull final List<Section> sections, String courseName) {
    if (sections.isEmpty()) {
      return false;
    }
    final String firstSectionTitle = sections.get(0).getName();
    if (firstSectionTitle != null && firstSectionTitle.equals(courseName)) {
      if (sections.size() == 1) {
        return false;
      }
      final String secondSectionTitle = sections.get(1).getName();
      if (sections.size() == 2 && (secondSectionTitle.equals(EduNames.ADDITIONAL_MATERIALS) ||
                                   secondSectionTitle.equals(StepikNames.PYCHARM_ADDITIONAL))) {
        return false;
      }
    }
    return true;
  }

  @NotNull
  public static List<Lesson> getLessons(String[] unitIds) throws IOException {
    List<UnitContainer> unitContainers = multipleRequestToStepik(StepikNames.UNITS, unitIds, UnitContainer.class);
    Stream<Unit> allUnits = unitContainers.stream().flatMap(container -> container.units.stream());
    String[] lessonIds = allUnits.map(unit -> String.valueOf(unit.lesson)).toArray(String[]::new);

    List<LessonContainer> lessonContainers = multipleRequestToStepik(StepikNames.LESSONS, lessonIds, LessonContainer.class);
    List<Lesson> lessons = lessonContainers.stream().flatMap(lessonContainer -> lessonContainer.lessons.stream()).collect(Collectors.toList());
    List<Unit> units = unitContainers.stream().flatMap(container -> container.units.stream()).collect(Collectors.toList());

    for (int i = 0; i < lessons.size(); i++) {
      Lesson lesson = lessons.get(i);
      Unit unit = units.get(i);
      if (!StepikUpdateDateExt.isSignificantlyAfter(lesson.getUpdateDate(), unit.getUpdateDate())) {
        lesson.setUpdateDate(unit.getUpdateDate());
      }
    }

    return sortLessonsByUnits(units, lessons);
  }

  /**
   * Stepik sorts result of multiple requests by id, but in some cases unit-wise and lessonId-wise order differ.
   * So we need to sort lesson by units to keep correct course structure
   */
  @NotNull
  private static List<Lesson> sortLessonsByUnits(List<Unit> units, List<Lesson> lessons) {
    HashMap<Integer, Lesson> idToLesson = new HashMap<>();
    units.sort(Comparator.comparingInt(unit -> unit.section));
    for (Lesson lesson : lessons) {
      idToLesson.put(lesson.getId(), lesson);
    }
    List<Lesson> sorted = new ArrayList<>();
    for (Unit unit : units) {
      int lessonId = unit.lesson;
      sorted.add(idToLesson.get(lessonId));
    }
    return sorted;
  }

  @VisibleForTesting
  public static List<Lesson> getLessonsFromUnits(RemoteCourse remoteCourse, String[] unitIds, boolean updateIndicator) throws IOException {
    final ProgressIndicator progressIndicator = ProgressManager.getInstance().getProgressIndicator();
    final List<Lesson> lessons = new ArrayList<>();
    List<Lesson> lessonsFromUnits = getLessons(unitIds);

    final int lessonCount = lessonsFromUnits.size();
    for (int lessonIndex = 0; lessonIndex < lessonCount; lessonIndex++) {
      Lesson lesson = lessonsFromUnits.get(lessonIndex);
      lesson.unitId = Integer.parseInt(unitIds[lessonIndex]);
      if (progressIndicator != null && updateIndicator) {
        final int readableIndex = lessonIndex + 1;
        progressIndicator.checkCanceled();
        progressIndicator.setText("Loading lesson " + readableIndex + " from " + lessonCount);
        progressIndicator.setFraction((double)readableIndex / lessonCount);
      }
      String[] stepIds = lesson.steps.stream().map(stepId -> String.valueOf(stepId)).toArray(String[]::new);
      List<StepSource> allStepSources = getStepSources(stepIds, remoteCourse.getLanguageID());

      if (!allStepSources.isEmpty()) {
        final StepOptions options = allStepSources.get(0).block.options;
        if (options != null && options.lessonType != null) {
          // TODO: find a better way to get framework lessons from stepik
          lesson = new FrameworkLesson(lesson);
        }
      }
      List<Task> tasks = getTasks(remoteCourse.getLanguageById(), lesson, stepIds, allStepSources);
      lesson.taskList.addAll(tasks);
      lessons.add(lesson);
    }

    return lessons;
  }

  public static List<StepSource> getStepSources(String[] stepIds, String language) throws IOException {
    Map<Key, Object> params = Collections.singletonMap(COURSE_LANGUAGE, language);
    List<StepContainer> stepContainers = multipleRequestToStepik(StepikNames.STEPS, stepIds, StepContainer.class, params);
    return stepContainers.stream().flatMap(stepContainer -> stepContainer.steps.stream()).collect(Collectors.toList());
  }

  @NotNull
  public static List<Task> getTasks(@NotNull Language language, @NotNull Lesson lesson, String[] stepIds, List<StepSource> allStepSources) {
    List<Task> tasks = new ArrayList<>();
    for (int i = 0; i < allStepSources.size(); i++) {
      StepSource step = allStepSources.get(i);
      Integer stepId = Integer.valueOf(stepIds[i]);
      StepikUser user = EduSettings.getInstance().getUser();
      StepikTaskBuilder builder = new StepikTaskBuilder(language, lesson, step, stepId, user == null ? -1 : user.getId());
      if (builder.isSupported(step.block.name)) {
        final Task task = builder.createTask(step.block.name);
        if (task != null) {
          tasks.add(task);
        }
      }
    }
    return tasks;
  }

  private static <T> T getFromStepik(String link, final Class<T> container) throws IOException {
    return getFromStepik(link, container, null);
  }

  private static <T> T getFromStepik(String link, final Class<T> container, @Nullable Map<Key, Object> params) throws IOException {
    if (EduSettings.isLoggedIn()) {
      final StepikUser user = EduSettings.getInstance().getUser();
      assert user != null;
      return StepikAuthorizedClient.getFromStepik(link, container, user, params);
    }
    return StepikClient.getFromStepik(link, container, params);
  }

  /**
   * Parses solution from Stepik.
   *
   * In Stepik solution text placeholder text is wrapped in <placeholder> tags. Here we're trying to find corresponding
   * placeholder for all taskFile placeholders.
   *
   * If we can't find at least one placeholder, we mark all placeholders as invalid. Invalid placeholder isn't showing
   * and task file with such placeholders couldn't be checked.
   *
   * @param taskFile for which we're updating placeholders
   * @param solutionFile from Stepik with text of last submission
   * @return false if there're invalid placeholders
   */
  static boolean setPlaceholdersFromTags(@NotNull TaskFile taskFile, @NotNull SolutionFile solutionFile) {
    int lastIndex = 0;
    StringBuilder builder = new StringBuilder(solutionFile.text);
    List<AnswerPlaceholder> placeholders = taskFile.getAnswerPlaceholders();
    boolean isPlaceholdersValid = true;
    for (AnswerPlaceholder placeholder : placeholders) {
      int start = builder.indexOf(OPEN_PLACEHOLDER_TAG, lastIndex);
      int end = builder.indexOf(CLOSE_PLACEHOLDER_TAG, start);
      if (start == -1 || end == -1) {
        isPlaceholdersValid = false;
        break;
      }
      placeholder.setOffset(start);
      String placeholderText = builder.substring(start + OPEN_PLACEHOLDER_TAG.length(), end);
      placeholder.setLength(placeholderText.length());
      builder.delete(end, end + CLOSE_PLACEHOLDER_TAG.length());
      builder.delete(start, start + OPEN_PLACEHOLDER_TAG.length());
      lastIndex = start + placeholderText.length();
    }

    if (!isPlaceholdersValid) {
      for (AnswerPlaceholder placeholder : placeholders) {
        markInvalid(placeholder);
      }
    }

    return isPlaceholdersValid;
  }

  private static void markInvalid(AnswerPlaceholder placeholder) {
    placeholder.setLength(-1);
    placeholder.setOffset(-1);
  }

  static String removeAllTags(@NotNull String text) {
    String result = text.replaceAll(OPEN_PLACEHOLDER_TAG, "");
    result = result.replaceAll(CLOSE_PLACEHOLDER_TAG, "");
    return result;
  }

  @Nullable
  static Reply getLastSubmission(@NotNull String stepId, boolean isSolved, String language) throws IOException {
    try {
      URI url = new URIBuilder(StepikNames.SUBMISSIONS)
        .addParameter("order", "desc")
        .addParameter("page", "1")
        .addParameter("status", isSolved ? "correct" : "wrong")
        .addParameter("step", stepId).build();
      Map<Key, Object> params = Collections.singletonMap(COURSE_LANGUAGE, language);
      Submission[] submissions = getFromStepik(url.toString(), SubmissionsWrapper.class, params).submissions;
      if (submissions.length > 0) {
        return submissions[0].reply;
      }
    }
    catch (URISyntaxException e) {
      LOG.warn(e.getMessage());
    }
    return null;
  }

  @NotNull
  static HashMap<String, String> getSolutionForStepikAssignment(@NotNull Task task, boolean isSolved) throws IOException {
    HashMap<String, String> taskFileToText = new HashMap<>();
    try {
      URI url = new URIBuilder(StepikNames.SUBMISSIONS)
        .addParameter("order", "desc")
        .addParameter("page", "1")
        .addParameter("status", isSolved ? "correct" : "wrong")
        .addParameter("step", String.valueOf(task.getStepId())).build();
      Submission[] submissions = getFromStepik(url.toString(), SubmissionsWrapper.class).submissions;
      Language language = task.getLesson().getCourse().getLanguageById();
      String stepikLanguage = StepikLanguages.langOfId(language.getID()).getLangName();
      for (Submission submission : submissions) {
        Reply reply = submission.reply;
        if (stepikLanguage != null && stepikLanguage.equals(reply.language)) {
          Collection<TaskFile> taskFiles = task.getTaskFiles().values();
          assert taskFiles.size() == 1;
          for (TaskFile taskFile : taskFiles) {
            taskFileToText.put(taskFile.getName(), reply.code);
          }
        }
      }
    }
    catch (URISyntaxException e) {
      LOG.warn(e.getMessage());
    }

    return taskFileToText;
  }

  public static StepSource getStep(int step) throws IOException {
    return getFromStepik(StepikNames.STEPS + step,
                         StepContainer.class).steps.get(0);
  }

  @Nullable
  public static Boolean[] taskStatuses(String[] progresses) {
    try {
      List<ProgressContainer> progressContainers = multipleRequestToStepik(StepikNames.PROGRESS, progresses, ProgressContainer.class);
      Map<String, Boolean> progressMap = progressContainers.stream()
        .flatMap(progressContainer -> progressContainer.progresses.stream())
        .collect(Collectors.toMap(p -> p.id, p -> p.isPassed));
      return Arrays.stream(progresses)
        .map(progressMap::get)
        .toArray(Boolean[]::new);
    }
    catch (IOException e) {
      LOG.warn(e.getMessage());
    }

    return null;
  }

  public static <T> List<T> multipleRequestToStepik(String apiUrl, String[] ids, final Class<T> container) throws IOException {
    return multipleRequestToStepik(apiUrl, ids, container, null);
  }

  private static <T> List<T> multipleRequestToStepik(String apiUrl, String[] ids,
                                                     final Class<T> container,
                                                     @Nullable Map<Key, Object> params) throws IOException {
    List<T> result = new ArrayList<>();

    int length = ids.length;
    for (int i = 0; i < length ; i += MAX_REQUEST_PARAMS) {
      String link;
      try {
        URIBuilder builder = new URIBuilder(apiUrl);
        List<String> sublist = Arrays.asList(ids).subList(i, Math.min(i + MAX_REQUEST_PARAMS, length));
        for (String id : sublist) {
          builder.addParameter("ids[]", id);
        }
        link = builder.build().toString();

      }
      catch (URISyntaxException e) {
        LOG.error(e.getMessage());
        continue;
      }
      result.add(getFromStepik(link, container, params));
    }

    return result;
  }

  public static void postSolution(@NotNull final Task task, boolean passed, @NotNull final Project project) {
    if (task.getStepId() <= 0) {
      return;
    }

    try {
      final String response = postAttempt(task.getStepId());
      if (response.isEmpty()) return;
      final Gson gson = new GsonBuilder().addDeserializationExclusionStrategy(ourExclusionStrategy).create();
      final AttemptWrapper.Attempt attempt = gson.fromJson(response, AttemptContainer.class).attempts.get(0);
      final ArrayList<SolutionFile> files = new ArrayList<>();
      final VirtualFile taskDir = task.getTaskDir(project);
      if (taskDir == null) {
        LOG.error("Failed to find task directory " + task.getName());
        return;
      }
      for (TaskFile taskFile : task.getTaskFiles().values()) {
        final String fileName = taskFile.getName();
        final VirtualFile virtualFile = EduUtils.findTaskFileInDir(taskFile, taskDir);
        if (virtualFile != null) {
          ApplicationManager.getApplication().runReadAction(() -> {
            final Document document = FileDocumentManager.getInstance().getDocument(virtualFile);
            if (document != null) {
              String text = document.getText();
              int insertedTextLength = 0;
              StringBuilder builder = new StringBuilder(text);
              for (AnswerPlaceholder placeholder : taskFile.getAnswerPlaceholders()) {
                builder.insert(placeholder.getOffset() + insertedTextLength, OPEN_PLACEHOLDER_TAG);
                builder.insert(placeholder.getOffset() + insertedTextLength + placeholder.getLength() + OPEN_PLACEHOLDER_TAG.length(),
                               CLOSE_PLACEHOLDER_TAG);
                insertedTextLength += OPEN_PLACEHOLDER_TAG.length() + CLOSE_PLACEHOLDER_TAG.length();
              }
              files.add(new SolutionFile(fileName, builder.toString()));
            }
          });
        }
      }

      postSubmission(passed, attempt, files, task);
    }
    catch (IOException e) {
      LOG.error(e.getMessage());
    }
  }

  static String postAttempt(int id) throws IOException {
    final CloseableHttpClient client = StepikAuthorizedClient.getHttpClient();
    if (client == null || !EduSettings.isLoggedIn()) return "";
    final HttpPost attemptRequest = new HttpPost(StepikNames.STEPIK_API_URL + StepikNames.ATTEMPTS);
    String attemptRequestBody = new Gson().toJson(new AttemptWrapper(id));
    attemptRequest.setEntity(new StringEntity(attemptRequestBody, ContentType.APPLICATION_JSON));

    final CloseableHttpResponse attemptResponse = client.execute(attemptRequest);
    final HttpEntity responseEntity = attemptResponse.getEntity();
    final String attemptResponseString = responseEntity != null ? EntityUtils.toString(responseEntity) : "";
    final StatusLine statusLine = attemptResponse.getStatusLine();
    EntityUtils.consume(responseEntity);
    if (statusLine.getStatusCode() != HttpStatus.SC_CREATED) {
      LOG.warn("Failed to make attempt " + attemptResponseString);
      return "";
    }
    return attemptResponseString;
  }

  private static void postSubmission(boolean passed, AttemptWrapper.Attempt attempt,
                                     ArrayList<SolutionFile> files, Task task) throws IOException {
    final HttpPost request = new HttpPost(StepikNames.STEPIK_API_URL + StepikNames.SUBMISSIONS);
    String requestBody = new Gson().toJson(new SubmissionWrapper(attempt.id, passed ? "1" : "0", files, task));
    request.setEntity(new StringEntity(requestBody, ContentType.APPLICATION_JSON));
    final CloseableHttpClient client = StepikAuthorizedClient.getHttpClient();
    if (client == null) return;
    final CloseableHttpResponse response = client.execute(request);
    final HttpEntity responseEntity = response.getEntity();
    final String responseString = responseEntity != null ? EntityUtils.toString(responseEntity) : "";
    final StatusLine line = response.getStatusLine();
    EntityUtils.consume(responseEntity);
    if (line.getStatusCode() != HttpStatus.SC_CREATED) {
      LOG.error("Failed to make submission " + responseString);
    }
  }

  @NotNull
  private static String createOAuthLink(String authRedirectUrl) {
    return "https://stepik.org/oauth2/authorize/" +
           "?client_id=" + StepikNames.CLIENT_ID +
           "&redirect_uri=" + authRedirectUrl +
           "&response_type=code";
  }

  @NotNull
  public static String getOAuthRedirectUrl() {
    if (EduUtils.isAndroidStudio()) {
      final CustomAuthorizationServer startedServer = CustomAuthorizationServer.getServerIfStarted(StepikNames.STEPIK);

      if (startedServer != null) {
        return startedServer.getHandlingUri();
      }

      try {
        return CustomAuthorizationServer.create(
          StepikNames.STEPIK,
          "",
          StepikConnector::codeHandler
        ).getHandlingUri();
      } catch (IOException e) {
        LOG.warn(e.getMessage());
        return StepikNames.EXTERNAL_REDIRECT_URL;
      }
    } else {
      int port = BuiltInServerManager.getInstance().getPort();

      // according to https://confluence.jetbrains.com/display/IDEADEV/Remote+communication
      int defaultPort = BuiltInServerOptions.getInstance().builtInServerPort;
      if (port >= defaultPort && port < (defaultPort + 20)) {
        return "http://localhost:" + port + "/api/" + StepikNames.OAUTH_SERVICE_NAME;
      }
    }

    return StepikNames.EXTERNAL_REDIRECT_URL;
  }

  private static String codeHandler(@NotNull String code, @NotNull String redirectUri) {
    final StepikUser user = StepikAuthorizedClient.login(code, redirectUri);
    if (user != null) {
      EduSettings.getInstance().setUser(user);
      return null;
    }
    return "Couldn't get user info";
  }

  public static void doAuthorize(@NotNull Runnable externalRedirectUrlHandler) {
    String redirectUrl = getOAuthRedirectUrl();
    String link = createOAuthLink(redirectUrl);
    BrowserUtil.browse(link);
    if (!redirectUrl.startsWith("http://localhost")) {
      externalRedirectUrlHandler.run();
    }
  }

  public static Unit getUnit(int unitId) {
    try {
      List<Unit> units =
        getFromStepik(StepikNames.UNITS + "/" + unitId, UnitContainer.class).units;
      if (!units.isEmpty()) {
        return units.get(0);
      }
    }
    catch (IOException e) {
      LOG.warn("Failed getting unit: " + unitId);
    }
    return new Unit();
  }

  @NotNull
  public static Section getSection(int sectionId) {
    try {
      List<Section> sections =
        getFromStepik(StepikNames.SECTIONS + "/" + sectionId, SectionContainer.class).getSections();
      if (!sections.isEmpty()) {
        return sections.get(0);
      }
    }
    catch (IOException e) {
      LOG.warn("Failed getting section: " + sectionId);
    }
    return new Section();
  }

  public static Lesson getLesson(int lessonId) {
    try {
      List<Lesson> lessons =
        getFromStepik(StepikNames.LESSONS + "/" + lessonId, LessonContainer.class).lessons;
      if (!lessons.isEmpty()) {
        return lessons.get(0);
      }
    }
    catch (IOException e) {
      LOG.warn("Failed getting section: " + lessonId);
    }
    return new Lesson();
  }

  public static void postTheory(Task task, final Project project) {
    if (!EduSettings.isLoggedIn()) {
      return;
    }
    final int stepId = task.getStepId();
    int lessonId = task.getLesson().getId();
    ProgressManager.getInstance().runProcessWithProgressAsynchronously(
      new Backgroundable(project, "Posting Theory to Stepik", false) {
        @Override
        public void run(@NotNull ProgressIndicator progressIndicator) {
          try {
            markStepAsViewed(lessonId, stepId);
          }
          catch (URISyntaxException | IOException e) {
            LOG.warn(e.getMessage());
          }
        }
      }, new EmptyProgressIndicator());
  }

  private static void markStepAsViewed(int lessonId, int stepId) throws URISyntaxException, IOException {
    final URI unitsUrl = new URIBuilder(StepikNames.UNITS).addParameter("lesson", String.valueOf(lessonId)).build();
    final UnitContainer unitContainer = getFromStepik(unitsUrl.toString(), UnitContainer.class);
    if (unitContainer.units.size() == 0) {
      LOG.warn("Got unexpected numbers of units: " + unitContainer.units.size());
      return;
    }

    final URIBuilder builder = new URIBuilder(StepikNames.ASSIGNMENTS);
    for (Integer assignmentId : unitContainer.units.get(0).assignments) {
      builder.addParameter("ids[]", String.valueOf(assignmentId));
    }

    final AssignmentsWrapper assignments = getFromStepik(builder.toString(), AssignmentsWrapper.class);
    if (assignments.assignments.size() > 0) {
      for (Assignment assignment : assignments.assignments) {
        if (assignment.step != stepId) {
          continue;
        }
        final HttpPost post = new HttpPost(StepikNames.STEPIK_API_URL + StepikNames.VIEWS);
        final ViewsWrapper viewsWrapper = new ViewsWrapper(assignment.id, stepId);
        post.setEntity(new StringEntity(new Gson().toJson(viewsWrapper)));
        post.addHeader(new BasicHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType()));

        CloseableHttpClient httpClient = StepikAuthorizedClient.getHttpClient();
        if (httpClient != null) {
          final CloseableHttpResponse viewPostResult = httpClient.execute(post);
          if (viewPostResult.getStatusLine().getStatusCode() != HttpStatus.SC_CREATED) {
            LOG.warn("Error while Views post, code: " + viewPostResult.getStatusLine().getStatusCode());
          }
        }
      }
    }
    else {
      LOG.warn("Got assignments of incorrect length: " + assignments.assignments.size());
    }
  }

  @NotNull
  private static List<Integer> getFeaturedCoursesIds() {
    return getCoursesIds(PROMOTED_COURSES_LINK);
  }

  private static List<Integer> getCoursesIds(@NotNull final String link) {
    try {
      final URL url = new URL(link);
      URLConnection conn = url.openConnection();
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
        return reader.lines().map(s -> Integer.valueOf(s.split("#")[0].trim())).collect(Collectors.toList());
      }
    } catch (IOException e) {
      LOG.warn("Failed to get courses from " + link);
    }
    return Lists.newArrayList();
  }

  @NotNull
  private static List<Integer> getInProgressCoursesIds() {
    return getCoursesIds(IN_PROGRESS_COURSES_LINK);
  }
}
