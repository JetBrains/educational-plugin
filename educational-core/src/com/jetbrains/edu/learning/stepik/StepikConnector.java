package com.jetbrains.edu.learning.stepik;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.intellij.ide.BrowserUtil;
import com.intellij.lang.Language;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task.Backgroundable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ConcurrencyUtil;
import com.intellij.util.containers.ContainerUtil;
import com.jetbrains.edu.learning.EduNames;
import com.jetbrains.edu.learning.EduSettings;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.authUtils.CustomAuthorizationServer;
import com.jetbrains.edu.learning.courseFormat.*;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.stepik.api.StepikNewConnector;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.builtInWebServer.BuiltInServerOptions;
import org.jetbrains.ide.BuiltInServerManager;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
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

  private StepikConnector() {
  }

  public static boolean loadCourseStructure(@NotNull final EduCourse remoteCourse) {
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

  public static void fillItems(@NotNull EduCourse remoteCourse) throws IOException {
    List<Integer> sectionIds = remoteCourse.getSectionIds();
    List<Section> allSections = StepikNewConnector.INSTANCE.getSections(sectionIds);

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

  private static List<StudyItem> getAllItems(@NotNull EduCourse remoteCourse, List<Callable<StudyItem>> tasks) {
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

  private static ArrayList<StudyItem> unpackTopLevelLessons(@NotNull EduCourse remoteCourse, List<StudyItem> sections) {
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

  private static void addTopLevelLessons(@NotNull EduCourse remoteCourse, List<Section> allSections)
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
  private static StudyItem loadItemTask(@NotNull EduCourse remoteCourse, Section section, int finalIndex)
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

  @NotNull
  public static List<Unit> getUnits(String[] unitIds) throws IOException {
    List<UnitContainer> unitContainers = multipleRequestToStepik(StepikNames.UNITS, unitIds, UnitContainer.class);
    return unitContainers.stream().flatMap(container -> container.units.stream()).collect(Collectors.toList());
  }

  private static List<Lesson> getLessons(EduCourse remoteCourse) throws IOException {
    String[] unitIds = getUnitsIds(remoteCourse);
    if (unitIds.length > 0) {
      return getLessonsFromUnits(remoteCourse, unitIds, true);
    }

    return Collections.emptyList();
  }

  public static List<Lesson> getLessons(EduCourse remoteCourse, int sectionId) throws IOException {
    final Section firstSection = StepikNewConnector.INSTANCE.getSection(sectionId);
    if (firstSection == null) {
      return Collections.emptyList();
    }
    String[] unitIds = firstSection.units.stream().map(id -> String.valueOf(id)).toArray(String[]::new);
    return new ArrayList<>(getLessonsFromUnits(remoteCourse, unitIds, true));
  }

  public static String[] getUnitsIds(EduCourse remoteCourse) throws IOException {
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
  public static List<Lesson> getLessonsFromUnits(EduCourse remoteCourse, String[] unitIds, boolean updateIndicator) throws IOException {
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
      List<StepikSteps.StepSource> allStepSources = getStepSources(stepIds, remoteCourse.getLanguageID());

      if (!allStepSources.isEmpty()) {
        final StepikSteps.StepOptions options = allStepSources.get(0).block.options;
        if (options != null && options.lessonType != null) {
          // TODO: find a better way to get framework lessons from stepik
          lesson = new FrameworkLesson(lesson);
        }
      }
      List<Task> tasks = getTasks(remoteCourse.getLanguageById(), lesson, allStepSources);
      lesson.taskList.addAll(tasks);
      lessons.add(lesson);
    }

    return lessons;
  }

  public static List<StepikSteps.StepSource> getStepSources(String[] stepIds, String language) throws IOException {
    Map<Key, Object> params = Collections.singletonMap(COURSE_LANGUAGE, language);
    List<StepikSteps.StepsList> stepContainers = multipleRequestToStepik(StepikNames.STEPS, stepIds, StepikSteps.StepsList.class, params);
    return stepContainers.stream().flatMap(stepContainer -> stepContainer.steps.stream()).collect(Collectors.toList());
  }

  @NotNull
  public static List<Task> getTasks(@NotNull Language language, @NotNull Lesson lesson, List<StepikSteps.StepSource> allStepSources) {
    List<Task> tasks = new ArrayList<>();
    for (StepikSteps.StepSource step : allStepSources) {
      StepikUser user = EduSettings.getInstance().getUser();
      StepikTaskBuilder builder = new StepikTaskBuilder(language, lesson, step, step.id, user == null ? -1 : user.getId());
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

  public static StepikSteps.StepSource getStep(int step) throws IOException {
    return getFromStepik(StepikNames.STEPS + step,
                         StepikSteps.StepsList.class).steps.get(0);
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
    final boolean success = StepikAuthorizedClient.login(code, redirectUri);
    return success ? null : "Couldn't get user info";
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
}
