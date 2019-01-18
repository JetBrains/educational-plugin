package com.jetbrains.edu.learning.stepik;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.containers.ContainerUtil;
import com.jetbrains.edu.learning.EduNames;
import com.jetbrains.edu.learning.EduTestCase;
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder;
import com.jetbrains.edu.learning.courseFormat.FeedbackLink;
import com.jetbrains.edu.learning.courseFormat.Lesson;
import com.jetbrains.edu.learning.courseFormat.TaskFile;
import com.jetbrains.edu.learning.serialization.converter.TaskRoots;
import com.jetbrains.edu.learning.serialization.converter.TaskRootsKt;
import com.jetbrains.edu.learning.stepik.api.*;
import com.jetbrains.edu.learning.stepik.serialization.StepikReplyAdapter;
import com.jetbrains.edu.learning.stepik.serialization.StepikStepOptionsAdapter;
import com.jetbrains.edu.learning.stepik.serialization.StepikSubmissionTaskAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static com.jetbrains.edu.learning.serialization.SerializationUtils.Json.EDU_TASK;
import static com.jetbrains.edu.learning.serialization.SerializationUtils.Json.TASK;
import static com.jetbrains.edu.learning.stepik.StepikNames.PYCHARM_PREFIX;

public class StepikFormatTest extends EduTestCase {

  @NotNull
  @Override
  protected String getTestDataPath() {
    return "testData/stepik";
  }

  public void testFirstVersion() throws IOException {
    doStepOptionMigrationTest(2);
  }

  public void testSecondVersion() throws IOException {
    doStepOptionMigrationTest(3);
  }

  public void testThirdVersion() throws IOException {
    doStepOptionMigrationTest(4);
  }

  public void testFifthVersion() throws IOException {
    doStepOptionMigrationTest(6);
  }

  public void testSixthVersion() throws IOException {
    for (Map.Entry<String, TaskRoots> entry : TaskRootsKt.LANGUAGE_TASK_ROOTS.entrySet()) {
      doStepOptionMigrationTest(7, entry.getKey(), getTestName(true) + ".gradle.after.json");
    }
    doStepOptionMigrationTest(7, EduNames.PYTHON, getTestName(true) + ".python.after.json");
  }

  public void test8Version() throws Exception {
    doStepOptionMigrationTest(9);
  }

  public void testAdditionalMaterialsLesson() throws IOException {
    String responseString = loadJsonText();
    final ObjectMapper mapper = StepikNewConnector.INSTANCE.getObjectMapper();
    final LessonsList lessonsList = mapper.readValue(responseString, LessonsList.class);

    Lesson lesson = lessonsList.lessons.get(0);
    assertEquals(EduNames.ADDITIONAL_MATERIALS, lesson.getName());
  }

  public void testAdditionalMaterialsStep() throws IOException {
    String responseString = loadJsonText();
    for (String language : Arrays.asList(EduNames.KOTLIN, EduNames.PYTHON)) {
      StepSource step = StepikClient.deserializeStepikResponse(StepsList.class,
                                                                              responseString,
                                                                              createParams(language)).steps.get(0);
      assertEquals(EduNames.ADDITIONAL_MATERIALS, step.getBlock().getOptions().getTitle());
      assertEquals("task_file.py", step.getBlock().getOptions().files.get(0).getName());
      assertEquals("test_helperq.py", step.getBlock().getOptions().getFiles().get(1).getName());
    }
  }

  public void testAvailableCourses() throws IOException {
    String responseString = loadJsonText();
    final ObjectMapper mapper = StepikNewConnector.INSTANCE.getObjectMapper();
    final CoursesList coursesList = mapper.readValue(jsonText, CoursesList.class);

    assertNotNull(coursesList.courses);
    assertEquals("Incorrect number of courses", 4, coursesList.courses.size());
  }

  public void testPlaceholderSerialization() throws IOException {
    final Gson gson = new GsonBuilder().setPrettyPrinting().excludeFieldsWithoutExposeAnnotation().create();
    AnswerPlaceholder answerPlaceholder = new AnswerPlaceholder();
    answerPlaceholder.setOffset(1);
    answerPlaceholder.setLength(10);
    answerPlaceholder.setPlaceholderText("type here");
    answerPlaceholder.setPossibleAnswer("answer1");
    answerPlaceholder.setHints(ContainerUtil.list("hint 1", "hint 2"));
    final String placeholderSerialization = gson.toJson(answerPlaceholder);
    String expected  = loadJsonText();
    JsonObject object = new JsonParser().parse(expected).getAsJsonObject();
    assertEquals(gson.toJson(gson.fromJson(object, AnswerPlaceholder.class)), placeholderSerialization);

  }

  public void testTokenUptoDate() throws IOException {
    String jsonText = loadJsonText();
    final ObjectMapper mapper = StepikNewConnector.INSTANCE.getObjectMapper();
    final UsersList usersList = mapper.readValue(jsonText, UsersList.class);
    assertNotNull(usersList);
    assertFalse(usersList.users.isEmpty());
    StepikUserInfo user = usersList.users.get(0);
    assertNotNull(user);
    assertFalse(user.isGuest());
  }

  public void testCourseAuthor() throws IOException {
    String jsonText = loadJsonText();
    final ObjectMapper mapper = StepikNewConnector.INSTANCE.getObjectMapper();
    final UsersList usersList = mapper.readValue(jsonText, UsersList.class);
    assertNotNull(usersList);
    assertFalse(usersList.users.isEmpty());
    StepikUserInfo user = usersList.users.get(0);
    assertNotNull(user);
    assertFalse(user.isGuest());
  }

  public void testSections() throws IOException {
    String jsonText = loadJsonText();
    final ObjectMapper mapper = StepikNewConnector.INSTANCE.getObjectMapper();
    final SectionsList sectionsList = mapper.readValue(jsonText, SectionsList.class);
    assertNotNull(sectionsList);
    assertEquals(1, sectionsList.sections.size());
    List<Integer> unitIds = sectionsList.sections.get(0).units;
    assertEquals(10, unitIds.size());
  }

  public void testUnit() throws IOException {
    String jsonText = loadJsonText();
    final ObjectMapper mapper = StepikNewConnector.INSTANCE.getObjectMapper();
    final UnitsList unitsList = mapper.readValue(jsonText, UnitsList.class);
    assertNotNull(unitsList);
    assertNotNull(unitsList);
    assertEquals(1, unitsList.units.size());
    final int lesson = unitsList.units.get(0).lesson;
    assertEquals(13416, lesson);
  }

  public void testLesson() throws IOException {
    String jsonText = loadJsonText();
    final ObjectMapper mapper = StepikNewConnector.INSTANCE.getObjectMapper();
    final LessonsList lessonsList = mapper.readValue(jsonText, LessonsList.class);

    assertNotNull(lessonsList);
    assertEquals(1, lessonsList.lessons.size());
    final Lesson lesson = lessonsList.lessons.get(0);
    assertNotNull(lesson);
  }

  public void testStep() throws IOException {
    Gson gson = getGson();
    String jsonText = loadJsonText();
    final StepsList stepContainer = gson.fromJson(jsonText, StepsList.class);
    assertNotNull(stepContainer);
    final StepSource step = stepContainer.steps.get(0);
    assertNotNull(step);
  }

  public void testStepBlock() throws IOException {
    Gson gson = getGson();
    String jsonText = loadJsonText();
    final StepsList stepContainer = gson.fromJson(jsonText, StepsList.class);
    final StepSource step = stepContainer.steps.get(0);
    final Step block = step.getBlock();
    assertNotNull(block);
    assertNotNull(block.getOptions());
    assertTrue(block.getName().startsWith(PYCHARM_PREFIX));
  }

  public void testStepBlockOptions() throws IOException {
    final StepOptions options = getStepOptions();
    assertNotNull(options);
  }

  public void testUpdateDate() throws IOException {
    Gson gson = getGson();
    String jsonText = loadJsonText();
    final StepsList stepContainer = gson.fromJson(jsonText, StepsList.class);
    final StepSource step = stepContainer.steps.get(0);
    assertNotNull(step.getUpdate_date());
  }

  public void testOptionsTitle() throws IOException {
    final StepOptions options = getStepOptions();
    assertEquals("Our first program", options.getTitle());
  }

  public void testOptionsDescription() throws IOException {
    final StepOptions options = getStepOptions();

    assertEquals("\n" +
        "Traditionally the first program you write in any programming language is <code>\"Hello World!\"</code>.\n" +
        "<br><br>\n" +
        "Introduce yourself to the World.\n" +
        "<br><br>\n" +
        "Hint: To run a script сhoose 'Run &lt;name&gt;' on the context menu. <br>\n" +
        "For more information visit <a href=\"https://www.jetbrains.com/help/pycharm/running-and-rerunning-applications.html\">our help</a>.\n" +
        "\n" +
        "<br>\n", options.getDescriptionText());
  }

  public void testOptionsFeedbackLinks() throws IOException {
    StepOptions stepOptions = getStepOptions();
    assertEquals(FeedbackLink.LinkType.CUSTOM, stepOptions.getMyFeedbackLink().getType());
  }

  public void testOptionsFiles() throws IOException {
    final StepOptions options = getStepOptions();

    final List<TaskFile> files = options.getFiles();
    assertEquals(2, files.size());
    final TaskFile taskFile1 = files.get(0);
    assertEquals("hello_world.py", taskFile1.getName());
    assertEquals("print(\"Hello, world! My name is type your name\")\n", taskFile1.getText());

    final TaskFile taskFile2 = files.get(1);
    assertEquals("tests.py", taskFile2.getName());
    assertEquals("from test_helper import run_common_tests, failed, passed, get_answer_placeholders\n" +
                 "\n" +
                 "\n" +
                 "def test_ASCII():\n" +
                 "    windows = get_answer_placeholders()\n" +
                 "    for window in windows:\n" +
                 "        all_ascii = all(ord(c) < 128 for c in window)\n" +
                 "        if not all_ascii:\n" +
                 "            failed(\"Please use only English characters this time.\")\n" +
                 "            return\n" +
                 "    passed()\n", taskFile2.getText());
  }

  private StepOptions getStepOptions() throws IOException {
    return getStepOptions(null);
  }

  private StepOptions getStepOptions(@Nullable String language) throws IOException {
    Gson gson = getGson(createParams(language));
    String jsonText = loadJsonText();
    final StepsList stepContainer = gson.fromJson(jsonText, StepsList.class);
    final StepSource step = stepContainer.steps.get(0);
    final Step block = step.getBlock();
    return block.getOptions();
  }

  public void testOptionsPlaceholder() throws IOException {
    final StepOptions options = getStepOptions();
    final List<TaskFile> files = options.getFiles();
    final TaskFile taskFile = files.get(0);

    final List<AnswerPlaceholder> placeholders = taskFile.getAnswerPlaceholders();
    assertEquals(1, placeholders.size());
    final int offset = placeholders.get(0).getOffset();
    assertEquals(32, offset);
    final int length = placeholders.get(0).getLength();
    assertEquals(14, length);
    assertEquals("type your name", taskFile.getText().substring(offset, offset + length));
  }

  public void testTaskStatuses() throws IOException {
    String jsonText = loadJsonText();
    final ObjectMapper mapper = StepikNewConnector.INSTANCE.getObjectMapper();
    final ProgressesList progressesList = mapper.readValue(jsonText, ProgressesList.class);
    assertNotNull(progressesList);
    List<Progress> progressList = progressesList.progresses;
    assertNotNull(progressList);
    final Boolean[] statuses = progressList.stream().map(progress -> progress.isPassed()).toArray(Boolean[]::new);
    assertNotNull(statuses);
    assertEquals(50, statuses.length);
  }

  public void testAttempts() throws IOException {
    final ObjectMapper mapper = StepikNewConnector.INSTANCE.getObjectMapper();
    final AttemptsList attemptsList = mapper.readValue(loadJsonText(), AttemptsList.class);
    assertNotNull(attemptsList);
    List<? extends StepikWrappers.Attempt> attempts = attemptsList.attempts;
    assertNotNull(attempts);
    assertEquals(20, attempts.size());
    StepikWrappers.Attempt attempt1 = attempts.get(0);
    assertNull(attempt1.dataset);
    StepikWrappers.Attempt attempt2 = attempts.get(11);
    assertNotNull(attempt2.dataset);
  }

  public void testLastSubmission() throws IOException {
    String jsonText = loadJsonText();
    final ObjectMapper mapper = StepikNewConnector.INSTANCE.getObjectMapper();
    final SubmissionsList submissionsList = mapper.readValue(jsonText, SubmissionsList.class);
    assertNotNull(submissionsList);
    assertNotNull(submissionsList.submissions);
    assertEquals(20, submissionsList.submissions.size());
    final StepikWrappers.Reply reply = submissionsList.submissions.get(0).reply;
    assertNotNull(reply);
    List<StepikWrappers.SolutionFile> solutionFiles = reply.solution;
    assertEquals(1, solutionFiles.size());
    assertEquals("hello_world.py", solutionFiles.get(0).name);
    assertEquals("print(\"Hello, world! My name is type your name\")\n", solutionFiles.get(0).text);
  }

  public void testReplyTo7Version() throws IOException {
    for (Map.Entry<String, TaskRoots> entry : TaskRootsKt.LANGUAGE_TASK_ROOTS.entrySet()) {
      doReplyMigrationTest(7, getTestName(true) + ".gradle.after.json", entry.getKey());
    }

    doReplyMigrationTest(7, getTestName(true) + ".python.after.json", EduNames.PYTHON);
  }

  public void testReplyTo9Version() throws IOException {
    doReplyMigrationTest(9);
  }

  public void testNonEduTasks() throws IOException {
    Gson gson = getGson();
    String jsonText = loadJsonText();
    final StepsList stepContainer = gson.fromJson(jsonText, StepsList.class);
    assertNotNull(stepContainer);
    assertNotNull(stepContainer.steps);
    assertEquals(3, stepContainer.steps.size());
  }

  @NotNull
  private static Gson getGson() {
    return getGson(null);
  }

  @NotNull
  private static Gson getGson(@Nullable Map<Key, Object> params) {
    return StepikClient.createGson(params);
  }

  @NotNull
  private String loadJsonText() throws IOException {
    return loadJsonText(getTestFile());
  }

  @NotNull
  private String loadJsonText(@NotNull String fileName) throws IOException {
    return FileUtil.loadFile(new File(getTestDataPath(), fileName));
  }

  private void doStepOptionMigrationTest(int maxVersion) throws IOException {
    doStepOptionMigrationTest(maxVersion, null, null);
  }

  private void doStepOptionMigrationTest(int maxVersion, @Nullable String language, @Nullable String afterFileName) throws IOException {
    doMigrationTest(afterFileName, jsonBefore -> StepikStepOptionsAdapter.migrate(jsonBefore, maxVersion, language));
  }

  private void doReplyMigrationTest(int maxVersion) throws IOException {
    doReplyMigrationTest(maxVersion, null, null);
  }

  private void doReplyMigrationTest(int maxVersion, @Nullable String afterFileName, @Nullable String language) throws IOException {
    doMigrationTest(afterFileName, replyObject -> {
      int initialVersion = StepikReplyAdapter.migrate(replyObject, maxVersion, language);

      String eduTaskWrapperString = replyObject.get(EDU_TASK).getAsString();

      JsonParser parser = new JsonParser();
      JsonObject eduTaskWrapperObject = parser.parse(eduTaskWrapperString).getAsJsonObject();
      JsonObject eduTaskObject = eduTaskWrapperObject.getAsJsonObject(TASK);
      StepikSubmissionTaskAdapter.migrate(eduTaskObject, initialVersion, maxVersion, language);
      Gson gson = new Gson();
      String eduTaskWrapperStringAfter = gson.toJson(eduTaskWrapperObject);
      replyObject.addProperty(EDU_TASK, eduTaskWrapperStringAfter);
      return replyObject;
    });
  }

  private void doMigrationTest(@Nullable String afterFileName,
                               @NotNull Function<JsonObject, JsonObject> migrationAction) throws IOException {
    String responseString = loadJsonText();
    String afterExpected;
    if (afterFileName != null) {
      afterExpected = loadJsonText(afterFileName);
    } else {
      afterExpected = loadJsonText(getTestName(true) + ".after.json");
    }

    JsonParser parser = new JsonParser();
    JsonObject jsonBefore = parser.parse(responseString).getAsJsonObject();
    JsonObject jsonAfter = migrationAction.apply(jsonBefore);

    Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    String afterActual = gson.toJson(jsonAfter);
    assertEquals(afterExpected, afterActual);
  }

  @NotNull
  private String getTestFile() {
    return getTestName(true) + ".json";
  }

  private static Map<Key, Object> createParams(@Nullable String language) {
    return language == null ? null : Collections.singletonMap(StepikAuthorizer.COURSE_LANGUAGE, language);
  }
}
