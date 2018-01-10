package com.jetbrains.edu.learning.stepik;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.intellij.ide.BrowserUtil;
import com.intellij.lang.Language;
import com.intellij.lang.LanguageExtensionPoint;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task.Backgroundable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.StreamUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.edu.learning.*;
import com.jetbrains.edu.learning.courseFormat.*;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.stepic.StepikLanguages;
import com.jetbrains.edu.learning.stepic.StepikTaskBuilder;
import org.apache.http.*;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.config.SocketConfig;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.bootstrap.HttpServer;
import org.apache.http.impl.bootstrap.ServerBootstrap;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.util.EntityUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.builtInWebServer.BuiltInServerOptions;
import org.jetbrains.ide.BuiltInServerManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.jetbrains.edu.learning.stepik.StepikWrappers.*;

public class StepikConnector {
  private static final Logger LOG = Logger.getInstance(StepikConnector.class.getName());

  public static final int CURRENT_VERSION = 2;
  //this prefix indicates that course can be opened by educational plugin
  private static final String ADAPTIVE_NOTE =
    "\n\nInitially, the adaptive system may behave somewhat randomly, but the more problems you solve, the smarter it becomes!";
  private static final String NOT_VERIFIED_NOTE = "\n\nNote: We’re sorry, but this course feels a little incomplete. " +
      "If you are the owner of the course please <a href=\"mailto:Tatiana.Vasilyeva@jetbrains.com\">get in touch with us</a>, " +
      "we would like to verify this with you; we think with improvement this can be listed as a featured course in the future.";
  private static final String OPEN_PLACEHOLDER_TAG = "<placeholder>";
  private static final String CLOSE_PLACEHOLDER_TAG = "</placeholder>";
  private static final String PROMOTED_COURSES_LINK = "https://raw.githubusercontent.com/JetBrains/educational-plugin/master/featured_courses.txt";
  public static final int MAX_REQUEST_PARAMS = 100; // restriction of Stepik API for multiple requests

  private StepikConnector() {
  }

  public static boolean enrollToCourse(final int courseId, @Nullable final StepicUser user) {
    if (user == null) return false;
    HttpPost post = new HttpPost(StepikNames.STEPIK_API_URL + StepikNames.ENROLLMENTS);
    try {
      final EnrollmentWrapper enrollment = new EnrollmentWrapper(String.valueOf(courseId));
      post.setEntity(new StringEntity(new GsonBuilder().create().toJson(enrollment)));
      final CloseableHttpClient client = StepikAuthorizedClient.getHttpClient(user);
      CloseableHttpResponse response = client.execute(post);
      StatusLine line = response.getStatusLine();
      return line.getStatusCode() == HttpStatus.SC_CREATED;
    }
    catch (IOException e) {
      LOG.warn(e.getMessage());
    }
    return false;
  }

  @NotNull
  public static List<Course> getCourses(@Nullable StepicUser user) {
    List<Course> result = new ArrayList<>();
    final List<Integer> featuredCourses = getFeaturedCourses();
    try {
      int pageNumber = 1;
      while (addCoursesFromStepik(user, result, pageNumber, featuredCourses)) {
        pageNumber += 1;
      }
    }
    catch (IOException e) {
      LOG.warn("Cannot load course list " + e.getMessage());
    }
    return result;
  }

  public static Date getCourseUpdateDate(final int courseId) {
    final String url = StepikNames.COURSES + "/" + courseId;
    try {
      final List<RemoteCourse> courses = StepikClient.getFromStepik(url, CoursesContainer.class).courses;
      if (!courses.isEmpty()) {
        return courses.get(0).getUpdateDate();
      }
    }
    catch (IOException e) {
      LOG.warn("Could not retrieve course with id=" + courseId);
    }

    return null;
  }

  public static Date getLessonUpdateDate(final int lessonId) {
    final String url = StepikNames.LESSONS + "/" + lessonId;
    try {
      List<Lesson> lessons = StepikClient.getFromStepik(url, LessonContainer.class).lessons;
      if (!lessons.isEmpty()) {
        return lessons.get(0).getUpdateDate();
      }
    }
    catch (IOException e) {
      LOG.warn("Could not retrieve lesson with id=" + lessonId);
    }

    return null;
  }

  public static Date getTaskUpdateDate(final int taskId) {
    final String url = StepikNames.STEPS + String.valueOf(taskId);
    try {
      List<StepSource> steps = StepikClient.getFromStepik(url, StepContainer.class).steps;
      if (!steps.isEmpty()) {
        return steps.get(0).update_date;
      }
    }
    catch (IOException e) {
      LOG.warn("Could not retrieve an update date for a task with id=" + taskId);
    }

    return null;
  }

  public static CoursesContainer getCoursesFromStepik(@Nullable StepicUser user, URI url) throws IOException {
    final CoursesContainer coursesContainer;
    if (user != null) {
      coursesContainer = StepikAuthorizedClient.getFromStepik(url.toString(), CoursesContainer.class, user);
    }
    else {
      coursesContainer = StepikClient.getFromStepik(url.toString(), CoursesContainer.class);
    }
    return coursesContainer;
  }

  private static boolean addCoursesFromStepik(@Nullable StepicUser user, List<Course> result, int pageNumber,
                                              @NotNull List<Integer> featuredCourses) throws IOException {
    final URI url;
    try {
      url = new URIBuilder(StepikNames.COURSES).addParameter("is_idea_compatible", "true").
        addParameter("page", String.valueOf(pageNumber)).build();
    }
    catch (URISyntaxException e) {
      LOG.error(e.getMessage());
      return false;
    }
    final CoursesContainer coursesContainer = getCoursesFromStepik(user, url);
    addAvailableCourses(result, coursesContainer, featuredCourses);
    return coursesContainer.meta.containsKey("has_next") && coursesContainer.meta.get("has_next") == Boolean.TRUE;
  }

  @Nullable
  public static RemoteCourse getCourseFromStepik(@Nullable StepicUser user, int courseId, boolean isIdeaCompatible) throws IOException {
    final URI url;
    try {
      url = new URIBuilder(StepikNames.COURSES + "/" + courseId)
        .addParameter("is_idea_compatible", String.valueOf(isIdeaCompatible))
        .build();
    }
    catch (URISyntaxException e) {
      LOG.error(e.getMessage());
      return null;
    }
    final CoursesContainer coursesContainer = getCoursesFromStepik(user, url);

    if (coursesContainer != null && !coursesContainer.courses.isEmpty()) {
      return coursesContainer.courses.get(0);
    } else {
      return null;
    }
  }

  static void addAvailableCourses(List<Course> result, CoursesContainer coursesContainer,
                                  @NotNull List<Integer> featuredCourses) throws IOException {
    final List<RemoteCourse> courses = coursesContainer.courses;
    for (RemoteCourse info : courses) {
      if (!info.isAdaptive() && StringUtil.isEmptyOrSpaces(info.getType())) continue;
      StepikUtils.setCourseLanguage(info);

      if (canBeOpened(info)) {
        final ArrayList<StepicUser> authors = new ArrayList<>();
        for (Integer instructor : info.getInstructors()) {
          final StepicUser author = StepikClient.getFromStepik(StepikNames.USERS + String.valueOf(instructor),
                                                               AuthorWrapper.class).users.get(0);
          authors.add(author);
        }
        info.setAuthors(authors);

        if (info.isAdaptive()) {
          info.setDescription("This is a Stepik Adaptive course.\n\n" + info.getDescription() + ADAPTIVE_NOTE);
        }
        if (info.isPublic() && !featuredCourses.contains(info.getId())) {
          info.setDescription(info.getDescription() + NOT_VERIFIED_NOTE);
        }
        info.setVisibility(getVisibility(info, featuredCourses));
        result.add(info);
      }
    }
  }

  private static CourseVisibility getVisibility(@NotNull RemoteCourse course, @NotNull List<Integer> featuredCourses) {
    if (!course.isPublic()) {
      return CourseVisibility.PrivateVisibility.INSTANCE;
    }
    if (featuredCourses.contains(course.getId())) {
      return new CourseVisibility.FeaturedVisibility(featuredCourses.indexOf(course.getId()));
    }
    if (featuredCourses.isEmpty()) {
      return CourseVisibility.LocalVisibility.INSTANCE;
    }
    return CourseVisibility.PublicVisibility.INSTANCE;
  }

  public static RemoteCourse getCourseByLink(@NotNull StepicUser user, @NotNull String link) throws IOException {
    int courseId;
    try {
      courseId = Integer.parseInt(link);
    }
    catch (NumberFormatException e) {
      courseId = getCourseIdFromLink(link);
    }
    if (courseId != -1) {
      return getCourseFromStepik(user, courseId, false);
    }
    return null;
  }

  public static int getCourseIdFromLink(@NotNull String link) {
    try {
      URL url = new URL(link);
      String[] pathParts = url.getPath().split("/");
      for (int i = 0; i < pathParts.length; i++) {
        String part = pathParts[i];
        if (part.equals("course") && i + 1 < pathParts.length) {
          return Integer.parseInt(pathParts[i + 1]);
        }
      }
    }
    catch (MalformedURLException | NumberFormatException e) {
      LOG.warn(e.getMessage());
    }
    return -1;
  }

  static boolean canBeOpened(RemoteCourse courseInfo) {
    final ArrayList<String> supportedLanguages = new ArrayList<>();
    final List<LanguageExtensionPoint<EduConfigurator<?>>> extensions = EduConfiguratorManager.allExtensions();
    for (LanguageExtensionPoint extension : extensions) {
      String languageId = extension.getKey();
      supportedLanguages.add(languageId);
    }

    if (courseInfo.isAdaptive()) {
      return supportedLanguages.contains(courseInfo.getLanguageID());
    }

    String courseType = courseInfo.getType();
    final List<String> typeLanguage = StringUtil.split(courseType, " ");
    String prefix = typeLanguage.get(0);
    if (!supportedLanguages.contains(courseInfo.getLanguageID())) return false;
    if (typeLanguage.size() < 2 || !prefix.startsWith(StepikNames.PYCHARM_PREFIX)) {
      return false;
    }
    String versionString = prefix.substring(StepikNames.PYCHARM_PREFIX.length());
    if (versionString.isEmpty()) {
      return true;
    }
    try {
      Integer version = Integer.valueOf(versionString);
      return version <= CURRENT_VERSION;
    }
    catch (NumberFormatException e) {
      LOG.info("Wrong version format", e);
      return false;
    }
  }

  public static RemoteCourse getCourse(@NotNull final Project project, @NotNull final RemoteCourse remoteCourse) {
    final List<Lesson> lessons = remoteCourse.getLessons(true);
    if (!lessons.isEmpty()) return remoteCourse;
    if (!remoteCourse.isAdaptive()) {
      try {
        remoteCourse.addLessons(getLessons(remoteCourse, remoteCourse.getSections()));
        return remoteCourse;
      }
      catch (IOException e) {
        LOG.error("IOException " + e.getMessage());
      }
    }
    else {
      final Lesson lesson = new Lesson();
      lesson.setName(EduNames.ADAPTIVE);
      remoteCourse.addLesson(lesson);
      //TODO: more specific name?
      final Task recommendation = StepikAdaptiveConnector.getNextRecommendation(project, remoteCourse);
      if (recommendation != null) {
        lesson.addTask(recommendation);
      }
      return remoteCourse;
    }
    return null;
  }

  public static List<Lesson> getLessons(RemoteCourse remoteCourse, List<Integer> sections) throws IOException {
    String[] sectionIds = sections.stream().map(id -> String.valueOf(id)).toArray(String[]::new);
    try {
      String[] unitIds = getUnitsIds(sectionIds);
      if (unitIds.length > 0) {
        return getLessonsFromUnits(remoteCourse, unitIds);
      }
    }
    catch (URISyntaxException e) {
      LOG.warn(e.getMessage());
    }

    return Collections.emptyList();
  }

  public static List<Lesson> getLessons(RemoteCourse remoteCourse, int sectionId) throws IOException {
    final SectionContainer sectionContainer = getFromStepik(StepikNames.SECTIONS + String.valueOf(sectionId),
            SectionContainer.class);
    if (sectionContainer.sections.isEmpty()) {
      return Collections.emptyList();
    }
    Section firstSection = sectionContainer.sections.get(0);
    String[] unitIds = firstSection.units.stream().map(id -> String.valueOf(id)).toArray(String[]::new);

    return new ArrayList<>(getLessonsFromUnits(remoteCourse, unitIds));
  }

  private static String[] getUnitsIds(String[] sectionIds) throws IOException, URISyntaxException {
    List<SectionContainer> containers = multipleRequestToStepik(StepikNames.SECTIONS, sectionIds, SectionContainer.class);
    Stream<Section> allSections = containers.stream().map(container -> container.sections).flatMap(sections -> sections.stream());
    return allSections
            .map(section -> section.units)
            .flatMap(unitList -> unitList.stream())
            .map(unit -> String.valueOf(unit))
            .toArray(String[]::new);
  }

  @NotNull
  private static List<Lesson> getLessons(String[] unitIds) throws IOException, URISyntaxException {
    List<UnitContainer> unitContainers = multipleRequestToStepik(StepikNames.UNITS, unitIds, UnitContainer.class);
    Stream<Unit> allUnits = unitContainers.stream().flatMap(container -> container.units.stream());
    String[] lessonIds = allUnits.map(unit -> String.valueOf(unit.lesson)).toArray(String[]::new);

    List<LessonContainer> lessonContainers = multipleRequestToStepik(StepikNames.LESSONS, lessonIds, LessonContainer.class);
    List<Lesson> lessons = lessonContainers.stream().flatMap(lessonContainer -> lessonContainer.lessons.stream()).collect(Collectors.toList());
    List<Unit> units = unitContainers.stream().flatMap(container -> container.units.stream()).collect(Collectors.toList());
    return sortLessonsByUnits(units, lessons);
  }

  /**
   * Stepik sorts result of multiple requests by id, but in some cases unit-wise and lessonId-wise order differ.
   * So we need to sort lesson by units to keep correct course structure
   */
  @NotNull
  private static List<Lesson> sortLessonsByUnits(List<Unit> units, List<Lesson> lessons) {
    HashMap<Integer, Lesson> idToLesson = new HashMap<>();
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

  private static List<Lesson> getLessonsFromUnits(RemoteCourse remoteCourse, String[] unitIds) throws IOException {
    final ProgressIndicator progressIndicator = ProgressManager.getInstance().getProgressIndicator();
    final List<Lesson> lessons = new ArrayList<>();
    try {
      List<Lesson> lessonsFromUnits = getLessons(unitIds);

      final int lessonCount = lessonsFromUnits.size();
      for (int lessonIndex = 0; lessonIndex < lessonCount; lessonIndex++) {
        Lesson lesson = lessonsFromUnits.get(lessonIndex);
        if (progressIndicator != null) {
          final int readableIndex = lessonIndex + 1;
          progressIndicator.checkCanceled();
          progressIndicator.setText("Loading lesson " + readableIndex + " from " + lessonCount);
          progressIndicator.setFraction((double)readableIndex / lessonCount);
        }
        String[] stepIds = lesson.steps.stream().map(stepId -> String.valueOf(stepId)).toArray(String[]::new);
        List<StepContainer> stepContainers = multipleRequestToStepik(StepikNames.STEPS, stepIds, StepContainer.class);
        List<StepSource> allStepSources = stepContainers.stream().flatMap(stepContainer -> stepContainer.steps.stream()).collect(Collectors.toList());

        for (int i = 0; i < allStepSources.size(); i++) {
          StepSource step = allStepSources.get(i);
          Integer stepId = Integer.valueOf(stepIds[i]);
          StepicUser user = EduSettings.getInstance().getUser();
          StepikTaskBuilder builder = new StepikTaskBuilder(remoteCourse, step, stepId, user == null ? -1 : user.getId());
          if (builder.isSupported(step.block.name)) {
            final Task task = builder.createTask(step.block.name);
            if (task != null) {
              lesson.addTask(task);
            }
          }
        }
        if (!lesson.taskList.isEmpty()) {
          lessons.add(lesson);
        }
      }
    }
    catch (URISyntaxException e) {
      LOG.warn(e.getMessage());
    }

    return lessons;
  }

  public static List<Language> getSupportedLanguages(RemoteCourse remoteCourse) {
    List<Language> languages = new ArrayList<>();
    try {
      Map<String, String> codeTemplates = getFirstCodeTemplates(remoteCourse);
      for (String languageName : codeTemplates.keySet()) {
        String id = StepikLanguages.langOfName(languageName).getId();
        Language language = Language.findLanguageByID(id);
        if (language != null) {
          languages.add(language);
        }
      }
    }
    catch (IOException | URISyntaxException  e) {
      LOG.warn(e.getMessage());
    }

    return languages;
  }

  @NotNull
  private static Map<String, String> getFirstCodeTemplates(@NotNull RemoteCourse remoteCourse) throws IOException, URISyntaxException {
    String[] unitsIds = getUnitsIds(remoteCourse.getSections().stream().map(section -> String.valueOf(section)).toArray(String[]::new));
    List<Lesson> lessons = getLessons(unitsIds);
    for (Lesson lesson : lessons) {
      String[] stepIds = lesson.steps.stream().map(stepId -> String.valueOf(stepId)).toArray(String[]::new);
      List<StepContainer> stepContainers = multipleRequestToStepik(StepikNames.STEPS, stepIds, StepContainer.class);
      List<StepSource> allStepSources = stepContainers.stream().flatMap(stepContainer -> stepContainer.steps.stream()).collect(Collectors.toList());

      for (StepSource stepSource : allStepSources) {
        Step step = stepSource.block;
        if (step != null && step.name.equals("code") && step.options != null) {
          Map<String, String> codeTemplates = step.options.codeTemplates;
          if (codeTemplates != null) {
            return codeTemplates;
          }
        }
      }
    }

    return Collections.emptyMap();
  }

  private static <T> T getFromStepik(String link, final Class<T> container) throws IOException {
    final StepicUser user = EduSettings.getInstance().getUser();
    final boolean isAuthorized = user != null;
    if (isAuthorized) {
      return StepikAuthorizedClient.getFromStepik(link, container, user);
    }
    return StepikClient.getFromStepik(link, container);
  }

  /**
   * Parses solution from Stepik.
   *
   * In Stepik solution text placeholder text is wrapped in <placeholder> tags. Here we're trying to find corresponding
   * placeholder for all taskFile placeholders.
   *
   * If we can't find at least one placholder, we mark all placeholders as invalid. Invalid placeholder isn't showing
   * and taskfile with such placeholders couldn't be checked.
   *
   * @param taskFile for which we're updating placeholders
   * @param solutionFile from Stepik with text of last submission
   * @return false if there're invalid placeholders
   */
  static boolean setPlaceholdersFromTags(@NotNull TaskFile taskFile, @NotNull SolutionFile solutionFile) {
    int lastIndex = 0;
    StringBuilder builder = new StringBuilder(solutionFile.text);
    List<AnswerPlaceholder> placeholders = taskFile.getActivePlaceholders();
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

  @NotNull
  static List<SolutionFile> getLastSubmission(@NotNull String stepId, boolean isSolved) throws IOException {
    try {
      URI url = new URIBuilder(StepikNames.SUBMISSIONS)
        .addParameter("order", "desc")
        .addParameter("page", "1")
        .addParameter("status", isSolved ? "correct" : "wrong")
        .addParameter("step", stepId).build();
      Submission[] submissions = getFromStepik(url.toString(), SubmissionsWrapper.class).submissions;
      if (submissions.length > 0) {
        List<SolutionFile> solutionFiles = submissions[0].reply.solution;
        if (solutionFiles != null) {
          return solutionFiles;
        }
      }
    }
    catch (URISyntaxException e) {
      LOG.warn(e.getMessage());
    }
    return Collections.emptyList();
  }

  @Nullable
  static String getSolutionForStepikAssignment(@NotNull Task task, boolean isSolved) throws IOException {
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
        Submission.Reply reply = submission.reply;
        if (stepikLanguage != null && stepikLanguage.equals(reply.language)) {
          return reply.code;
        }
      }
    }
    catch (URISyntaxException e) {
      LOG.warn(e.getMessage());
    }

    return null;
  }

  public static StepSource getStep(int step) throws IOException {
    return getFromStepik(StepikNames.STEPS + String.valueOf(step),
                         StepContainer.class).steps.get(0);
  }

  @Nullable
  static Boolean[] taskStatuses(String[] progresses) {
    try {
      List<ProgressContainer> progressContainers = multipleRequestToStepik(StepikNames.PROGRESS, progresses, ProgressContainer.class);
      List<ProgressContainer.Progress> progressList = progressContainers.stream().flatMap(progressContainer -> progressContainer.progresses.stream()).collect(Collectors.toList());
      return progressList.stream().map(progress -> progress.isPassed).toArray(Boolean[]::new);
    }
    catch (URISyntaxException | IOException e) {
      LOG.warn(e.getMessage());
    }

    return null;
  }

  private static <T> List<T> multipleRequestToStepik(String apiUrl, String[] ids, final Class<T> container) throws URISyntaxException, IOException {
    List<T> result = new ArrayList<>();

    int length = ids.length;
    for (int i = 0; i < length ; i += MAX_REQUEST_PARAMS) {
      URIBuilder builder = new URIBuilder(apiUrl);
      List<String> sublist = Arrays.asList(ids).subList(i, Math.min(i + MAX_REQUEST_PARAMS, length));
      for (String id : sublist) {
        builder.addParameter("ids[]", id);
      }
      String link = builder.build().toString();
      result.add(getFromStepik(link, container));
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
      final AttemptWrapper.Attempt attempt =
        new Gson().fromJson(response, AttemptContainer.class).attempts.get(0);
      final Map<String, TaskFile> taskFiles = task.getTaskFiles();
      final ArrayList<SolutionFile> files = new ArrayList<>();
      final VirtualFile taskDir = task.getTaskDir(project);
      if (taskDir == null) {
        LOG.error("Failed to find task directory " + task.getName());
        return;
      }
      for (TaskFile fileEntry : taskFiles.values()) {
        final String fileName = fileEntry.name;
        final VirtualFile virtualFile = taskDir.findFileByRelativePath(fileName);
        if (virtualFile != null) {
          ApplicationManager.getApplication().runReadAction(() -> {
            final Document document = FileDocumentManager.getInstance().getDocument(virtualFile);
            if (document != null) {
              String text = document.getText();
              int insertedTextLength = 0;
              StringBuilder builder = new StringBuilder(text);
              for (AnswerPlaceholder placeholder : fileEntry.getActivePlaceholders()) {
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

      postSubmission(passed, attempt, files);
    }
    catch (IOException e) {
      LOG.error(e.getMessage());
    }
  }

  public static String postAttempt(int id) throws IOException {
    final CloseableHttpClient client = StepikAuthorizedClient.getHttpClient();
    if (client == null || EduSettings.getInstance().getUser() == null) return "";
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
                                     ArrayList<SolutionFile> files) throws IOException {
    final HttpPost request = new HttpPost(StepikNames.STEPIK_API_URL + StepikNames.SUBMISSIONS);

    String requestBody = new Gson().toJson(new SubmissionWrapper(attempt.id, passed ? "1" : "0", files));
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
  public static String createOAuthLink(String authRedirectUrl) {
    return "https://stepik.org/oauth2/authorize/" +
           "?client_id=" + StepikNames.CLIENT_ID +
           "&redirect_uri=" + authRedirectUrl +
           "&response_type=code";
  }

  @NotNull
  public static String getOAuthRedirectUrl() {
    if (EduUtils.isAndroidStudio()) {
      CustomServerAuthorizer customServerAuthorizer = new CustomServerAuthorizer();
      int port = customServerAuthorizer.startServer();
      if (port != -1) {
        return "http://localhost:" + port;
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
        getFromStepik(StepikNames.UNITS + "/" + String.valueOf(unitId), UnitContainer.class).units;
      if (!units.isEmpty()) {
        return units.get(0);
      }
    }
    catch (IOException e) {
      LOG.warn("Failed getting unit: " + unitId);
    }
    return new Unit();
  }

  public static Section getSection(int sectionId) {
    try {
      List<Section> sections =
        getFromStepik(StepikNames.SECTIONS + "/" + String.valueOf(sectionId), SectionContainer.class).getSections();
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
        getFromStepik(StepikNames.LESSONS + "/" + String.valueOf(lessonId), LessonContainer.class).lessons;
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
    if (EduSettings.getInstance().getUser() == null) {
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
    final StepikWrappers.UnitContainer unitContainer = getFromStepik(unitsUrl.toString(), StepikWrappers.UnitContainer.class);
    if (unitContainer.units.size() == 0) {
      LOG.warn("Got unexpected numbers of units: " + unitContainer.units.size());
      return;
    }

    final URIBuilder builder = new URIBuilder(StepikNames.ASSIGNMENTS);
    for (Integer assignmentId : unitContainer.units.get(0).assignments) {
      builder.addParameter("ids[]", String.valueOf(assignmentId));
    }

    final StepikWrappers.AssignmentsWrapper assignments = getFromStepik(builder.toString(), StepikWrappers.AssignmentsWrapper.class);
    if (assignments.assignments.size() > 0) {
      for (StepikWrappers.Assignment assignment : assignments.assignments) {
        if (assignment.step != stepId) {
          continue;
        }
        final HttpPost post = new HttpPost(StepikNames.STEPIK_API_URL + StepikNames.VIEWS);
        final StepikWrappers.ViewsWrapper viewsWrapper = new StepikWrappers.ViewsWrapper(assignment.id, stepId);
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
  public static List<Integer> getFeaturedCourses() {
    try {
      final URL url = new URL(PROMOTED_COURSES_LINK);
      URLConnection conn = url.openConnection();
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
        return reader.lines().map(s -> Integer.valueOf(s.split("#")[0].trim())).collect(Collectors.toList());
      }
    } catch (IOException e) {
      LOG.warn("Failed to get promoted courses");
    }
    return Lists.newArrayList();
  }


  private static class CustomServerAuthorizer {
    private static final int DEFAULT_AUTH_SERVER_PORT = 36656;
    private static final int PORT_TO_TRY_NUMBER = 10;
    private HttpServer myServer;

    private int startServer() {
      int port = -1;
      for (int i = 0; i < PORT_TO_TRY_NUMBER; i++) {
        if (available(DEFAULT_AUTH_SERVER_PORT + i)) {
          port = DEFAULT_AUTH_SERVER_PORT + i;
          break;
        }
      }
      if (port != -1) {
        SocketConfig socketConfig = SocketConfig.custom()
                .setSoTimeout(15000)
                .setTcpNoDelay(true)
                .build();
        myServer = ServerBootstrap.bootstrap()
                .setListenerPort(port)
                .setServerInfo("Edu Tools Auth Server")
                .registerHandler("*", new MyContextHandler())
                .setSocketConfig(socketConfig)
                .create();
        try {
          myServer.start();
        }
        catch (IOException e) {
          LOG.warn(e.getMessage());
          return -1;
        }
      }

      return port;
    }

    private static boolean available(int port) {
      try (Socket ignored = new Socket("localhost", port)) {
        return false;
      }
      catch (IOException ignored) {
        return true;
      }
    }


    private class MyContextHandler implements HttpRequestHandler {

      private void stopServerInNewThread() {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
          try {
            LOG.info("Stopping server");
            myServer.stop();
            LOG.info("Server stopped");
          }
          catch (Exception e) {
            LOG.warn(e.getMessage());
          }
        });
      }

      @Override
      public void handle(HttpRequest httpRequest, HttpResponse httpResponse, HttpContext httpContext) throws IOException {
        LOG.info("Handling auth response");

        try {
          List<NameValuePair> parse = URLEncodedUtils.parse(new URI(httpRequest.getRequestLine().getUri()), "UTF-8");
          for (NameValuePair pair : parse) {
            if (pair.getName().equals("code")) {
              String code = pair.getValue();
              StepicUser user = StepikAuthorizedClient.login(code, "http://localhost:" + myServer.getLocalPort());
              if (user != null) {
                EduSettings.getInstance().setUser(user);
                sendResponse(httpResponse, "/oauthResponsePages/okPage.html");
              }
              else {
                sendResponse(httpResponse, "/oauthResponsePages/errorPage.html");
              }
              break;
            }
          }
        }
        catch (URISyntaxException e) {
          LOG.warn(e.getMessage());
          sendResponse(httpResponse, "/oauthResponsePages/errorPage.html");
        }
        finally {
          stopServerInNewThread();
        }
      }

      private void sendResponse(HttpResponse httpResponse, String pageAddress) throws IOException {
        InputStream pageTemplateStream = getClass().getResourceAsStream(pageAddress);
        String pageTemplate = StreamUtil.readText(pageTemplateStream, Charset.forName("UTF-8"));
        String pageWithProductName = pageTemplate.replaceAll("%IDE_NAME", ApplicationNamesInfo.getInstance().getFullProductName());
        httpResponse.setHeader("Content-Type", "text/html");
        httpResponse.setEntity(new StringEntity(pageWithProductName));
      }
    }
  }
}
