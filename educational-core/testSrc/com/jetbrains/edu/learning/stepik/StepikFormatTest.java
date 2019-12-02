package com.jetbrains.edu.learning.stepik;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtilRt;
import com.jetbrains.edu.learning.EduNames;
import com.jetbrains.edu.learning.EduTestCase;
import com.jetbrains.edu.learning.courseFormat.*;
import com.jetbrains.edu.learning.serialization.converter.TaskRoots;
import com.jetbrains.edu.learning.serialization.converter.TaskRootsKt;
import com.jetbrains.edu.learning.stepik.api.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static com.jetbrains.edu.learning.EduNames.TASK;
import static com.jetbrains.edu.learning.serialization.SerializationUtils.Json.EDU_TASK;
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
    for (Map.Entry<String, TaskRoots> ignored : TaskRootsKt.LANGUAGE_TASK_ROOTS.entrySet()) {
      doStepOptionMigrationTest(7, getTestName(true) + ".gradle.after.json");
    }
  }

  public void testSixthVersionPython() throws IOException {
    doStepOptionMigrationTest(7, getTestName(true) + ".after.json");
  }

  public void test8Version() throws Exception {
    doStepOptionMigrationTest(9);
  }

  public void test9Version() throws IOException {
    doStepOptionMigrationTest(10);
  }

  public void test10Version() throws IOException {
    doStepOptionMigrationTest(10);
  }

  public void testAdditionalCourseMaterials() throws IOException {
    String responseString = loadJsonText();
    final ObjectMapper mapper = StepikConnector.getInstance().getObjectMapper();
    final AdditionalCourseInfo additionalCourseInfo = mapper.readValue(responseString, AdditionalCourseInfo.class);
    assertEquals(1, additionalCourseInfo.additionalFiles.size());
    assertTrue(additionalCourseInfo.getSolutionsHidden());
  }

  public void testAdditionalLessonMaterials() throws IOException {
    String responseString = loadJsonText();
    final ObjectMapper mapper = StepikConnector.getInstance().getObjectMapper();
    final AdditionalLessonInfo additionalLessonInfo = mapper.readValue(responseString, AdditionalLessonInfo.class);
    assertEquals("renamed", additionalLessonInfo.getCustomName());
    assertEquals("My cool task", additionalLessonInfo.taskNames.get(123));
    assertEquals(1, additionalLessonInfo.taskFiles.size());
    assertEquals(3, additionalLessonInfo.taskFiles.get(123).size());
  }

  public void testAdditionalMaterialsStep() throws IOException {
    String responseString = loadJsonText();
    for (String ignored : Arrays.asList(EduNames.KOTLIN, EduNames.PYTHON)) {
      final ObjectMapper mapper = StepikConnector.getInstance().getObjectMapper();

      StepSource step = mapper.readValue(responseString, StepsList.class).steps.get(0);
      PyCharmStepOptions options = (PyCharmStepOptions)step.getBlock().getOptions();
      assertEquals(EduNames.ADDITIONAL_MATERIALS, options.getTitle());
      assertEquals("task_file.py", options.getFiles().get(0).getName());
      assertEquals("test_helperq.py", options.getFiles().get(1).getName());
    }
  }

  public void testAvailableCourses() throws IOException {
    String responseString = loadJsonText();
    final ObjectMapper mapper = StepikConnector.getInstance().getObjectMapper();
    final CoursesList coursesList = mapper.readValue(responseString, CoursesList.class);

    assertNotNull(coursesList.courses);
    assertEquals("Incorrect number of courses", 4, coursesList.courses.size());
  }

  public void testPlaceholderSerialization() throws IOException {
    AnswerPlaceholder answerPlaceholder = new AnswerPlaceholder();
    answerPlaceholder.setOffset(1);
    answerPlaceholder.setLength(10);
    answerPlaceholder.setPlaceholderText("type here");
    answerPlaceholder.setPossibleAnswer("answer1");
    answerPlaceholder.setHints(Arrays.asList("hint 1", "hint 2"));
    final ObjectMapper mapper = StepikConnector.getInstance().getObjectMapper();
    final String placeholderSerialization = mapper.writeValueAsString(answerPlaceholder);
    String expected  = loadJsonText();

    JsonNode object = mapper.readTree(expected);
    assertEquals(mapper.writeValueAsString(mapper.treeToValue(object, AnswerPlaceholder.class)), placeholderSerialization);

  }

  public void testTokenUptoDate() throws IOException {
    String jsonText = loadJsonText();
    final ObjectMapper mapper = StepikConnector.getInstance().getObjectMapper();
    final UsersList usersList = mapper.readValue(jsonText, UsersList.class);
    assertNotNull(usersList);
    assertFalse(usersList.users.isEmpty());
    StepikUserInfo user = usersList.users.get(0);
    assertNotNull(user);
  }

  public void testCourseAuthor() throws IOException {
    String jsonText = loadJsonText();
    final ObjectMapper mapper = StepikConnector.getInstance().getObjectMapper();
    final UsersList usersList = mapper.readValue(jsonText, UsersList.class);
    assertNotNull(usersList);
    assertFalse(usersList.users.isEmpty());
    StepikUserInfo user = usersList.users.get(0);
    assertNotNull(user);
  }

  public void testSections() throws IOException {
    String jsonText = loadJsonText();
    final ObjectMapper mapper = StepikConnector.getInstance().getObjectMapper();
    final SectionsList sectionsList = mapper.readValue(jsonText, SectionsList.class);
    assertNotNull(sectionsList);
    assertEquals(1, sectionsList.sections.size());
    List<Integer> unitIds = sectionsList.sections.get(0).units;
    assertEquals(10, unitIds.size());
  }

  public void testUnit() throws IOException {
    String jsonText = loadJsonText();
    final ObjectMapper mapper = StepikConnector.getInstance().getObjectMapper();
    final UnitsList unitsList = mapper.readValue(jsonText, UnitsList.class);
    assertNotNull(unitsList);
    assertNotNull(unitsList);
    assertEquals(1, unitsList.units.size());
    final int lesson = unitsList.units.get(0).getLesson();
    assertEquals(13416, lesson);
  }

  public void testLesson() throws IOException {
    String jsonText = loadJsonText();
    final ObjectMapper mapper = StepikConnector.getInstance().getObjectMapper();
    final LessonsList lessonsList = mapper.readValue(jsonText, LessonsList.class);

    assertNotNull(lessonsList);
    assertEquals(1, lessonsList.lessons.size());
    final Lesson lesson = lessonsList.lessons.get(0);
    assertNotNull(lesson);
  }

  public void testStep() throws IOException {
    String jsonText = loadJsonText();
    final ObjectMapper mapper = StepikConnector.getInstance().getObjectMapper();
    final StepsList stepContainer = mapper.readValue(jsonText, StepsList.class);
    assertNotNull(stepContainer);
    final StepSource step = stepContainer.steps.get(0);
    assertNotNull(step);
  }

  public void testStepBlock() throws IOException {
    String jsonText = loadJsonText();
    final ObjectMapper mapper = StepikConnector.getInstance().getObjectMapper();
    final StepsList stepContainer = mapper.readValue(jsonText, StepsList.class);
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
    String jsonText = loadJsonText();
    final ObjectMapper mapper = StepikConnector.getInstance().getObjectMapper();
    final StepsList stepContainer = mapper.readValue(jsonText, StepsList.class);
    final StepSource step = stepContainer.steps.get(0);
    assertNotNull(step.getUpdateDate());
  }

  public void testOptionsTitle() throws IOException {
    final PyCharmStepOptions options = getStepOptions();
    assertEquals("Our first program", options.getTitle());
  }

  public void testOptionsDescription() throws IOException {
    String jsonText = loadJsonText();
    final ObjectMapper mapper = StepikConnector.getInstance().getObjectMapper();
    final StepsList stepContainer = mapper.readValue(jsonText, StepsList.class);
    final StepSource step = stepContainer.steps.get(0);
    final Step block = step.getBlock();
    assertEquals("\n" +
        "Traditionally the first program you write in any programming language is <code>\"Hello World!\"</code>.\n" +
        "<br><br>\n" +
        "Introduce yourself to the World.\n" +
        "<br><br>\n" +
        "Hint: To run a script сhoose 'Run &lt;name&gt;' on the context menu. <br>\n" +
        "For more information visit <a href=\"https://www.jetbrains.com/help/pycharm/running-and-rerunning-applications.html\">our help</a>.\n" +
        "\n" +
        "<br>\n", block.getText());
  }

  public void testOptionsFeedbackLinks() throws IOException {
    PyCharmStepOptions stepOptions = getStepOptions();
    assertEquals(FeedbackLink.LinkType.CUSTOM, stepOptions.getMyFeedbackLink().getType());
  }

  public void testOptionsFiles() throws IOException {
    final PyCharmStepOptions options = getStepOptions();

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

  private PyCharmStepOptions getStepOptions() throws IOException {
    String jsonText = loadJsonText();
    final ObjectMapper mapper = StepikConnector.getInstance().getObjectMapper();
    final StepsList stepContainer = mapper.readValue(jsonText, StepsList.class);
    final StepSource step = stepContainer.steps.get(0);
    final Step block = step.getBlock();
    return (PyCharmStepOptions)block.getOptions();
  }

  public void testOptionsPlaceholder() throws IOException {
    final PyCharmStepOptions options = getStepOptions();
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

  public void testOptionsPlaceholderDependency() throws IOException {
    final PyCharmStepOptions options = getStepOptions();
    final List<TaskFile> files = options.getFiles();
    final TaskFile taskFile = files.get(0);

    final List<AnswerPlaceholder> placeholders = taskFile.getAnswerPlaceholders();
    assertEquals(1, placeholders.size());
    final AnswerPlaceholderDependency dependency = placeholders.get(0).getPlaceholderDependency();
    assertNotNull(dependency);
    assertEquals("mysite/settings.py", dependency.getFileName());
    assertEquals("task1", dependency.getTaskName());
    assertEquals("lesson1", dependency.getLessonName());
    assertEquals(1, dependency.getPlaceholderIndex());
  }

  public void testTaskStatuses() throws IOException {
    String jsonText = loadJsonText();
    final ObjectMapper mapper = StepikConnector.getInstance().getObjectMapper();
    final ProgressesList progressesList = mapper.readValue(jsonText, ProgressesList.class);
    assertNotNull(progressesList);
    List<Progress> progressList = progressesList.progresses;
    assertNotNull(progressList);
    final Boolean[] statuses = progressList.stream().map(progress -> progress.isPassed()).toArray(Boolean[]::new);
    assertNotNull(statuses);
    assertEquals(50, statuses.length);
  }

  public void testAttempts() throws IOException {
    final ObjectMapper mapper = StepikConnector.getInstance().getObjectMapper();
    final AttemptsList attemptsList = mapper.readValue(loadJsonText(), AttemptsList.class);
    assertNotNull(attemptsList);
    List<Attempt> attempts = attemptsList.attempts;
    assertNotNull(attempts);
    assertEquals(20, attempts.size());
    Attempt attempt1 = attempts.get(0);
    assertNull(attempt1.getDataset().getOptions());
    Attempt attempt2 = attempts.get(11);
    assertNotNull(attempt2.getDataset());
  }

  public void testLastSubmission() throws IOException {
    String jsonText = loadJsonText();
    final ObjectMapper mapper = StepikConnector.getInstance().getObjectMapper();
    final SubmissionsList submissionsList = mapper.readValue(jsonText, SubmissionsList.class);
    assertNotNull(submissionsList);
    assertNotNull(submissionsList.submissions);
    assertEquals(20, submissionsList.submissions.size());
    final Reply reply = submissionsList.submissions.get(0).getReply();
    assertNotNull(reply);
    List<SolutionFile> solutionFiles = reply.getSolution();
    assertEquals(1, solutionFiles.size());
    assertEquals("hello_world.py", solutionFiles.get(0).getName());
    assertEquals("print(\"Hello, world! My name is type your name\")\n", solutionFiles.get(0).getText());
  }

  public void testReplyTo7Version() throws IOException {
    for (Map.Entry<String, TaskRoots> entry : TaskRootsKt.LANGUAGE_TASK_ROOTS.entrySet()) {
      doReplyMigrationTest(7, getTestName(true) + ".gradle.after.json", entry.getKey());
    }
  }

  public void testReplyTo7VersionPython() throws IOException {
    doReplyMigrationTest(7, getTestName(true) + ".after.json", EduNames.PYTHON);
  }

  public void testReplyTo9Version() throws IOException {
    doReplyMigrationTest(9);
  }

  public void testReplyTo10Version() throws IOException {
    doReplyMigrationTest(10);
  }

  public void testNonEduTasks() throws IOException {
    String jsonText = loadJsonText();
    final ObjectMapper mapper = StepikConnector.getInstance().getObjectMapper();
    final StepsList stepContainer = mapper.readValue(jsonText, StepsList.class);
    assertNotNull(stepContainer);
    assertNotNull(stepContainer.steps);
    assertEquals(3, stepContainer.steps.size());
  }

  public void testTaskWithCustomName() throws IOException {
    String jsonText = loadJsonText();
    final ObjectMapper mapper = StepikConnector.getInstance().getObjectMapper();
    final StepsList stepContainer = mapper.readValue(jsonText, StepsList.class);
    assertNotNull(stepContainer);
    Step block = stepContainer.steps.get(0).getBlock();
    assertNotNull(block);
    PyCharmStepOptions options = (PyCharmStepOptions)block.getOptions();
    assertNotNull(options);
    assertEquals("custom name", options.getCustomPresentableName());
  }

  @NotNull
  private String loadJsonText() throws IOException {
    return loadJsonText(getTestFile());
  }

  @NotNull
  private String loadJsonText(@NotNull String fileName) throws IOException {
    return FileUtil.loadFile(new File(getTestDataPath(), fileName), true);
  }

  private void doStepOptionMigrationTest(int maxVersion) throws IOException {
    doStepOptionMigrationTest(maxVersion, null);
  }

  private void doStepOptionMigrationTest(int maxVersion, @Nullable String afterFileName) throws IOException {
    doMigrationTest(afterFileName, jsonBefore -> JacksonStepOptionsDeserializer.migrate(jsonBefore, maxVersion));
  }

  private void doReplyMigrationTest(int maxVersion) throws IOException {
    doReplyMigrationTest(maxVersion, null, null);
  }

  private void doReplyMigrationTest(int maxVersion, @Nullable String afterFileName, @Nullable String language) throws IOException {
    doMigrationTest(afterFileName, replyObject -> {
      int initialVersion = StepikReplyDeserializer.migrate(replyObject, maxVersion);

      String eduTaskWrapperString = replyObject.get(EDU_TASK).asText();

      try {
        final ObjectNode eduTaskWrapperObject = (ObjectNode)new ObjectMapper().readTree(eduTaskWrapperString);
        ObjectNode eduTaskObject = (ObjectNode)eduTaskWrapperObject.get(TASK);
        JacksonSubmissionDeserializer.migrate(eduTaskObject, initialVersion, maxVersion, language);
        final String eduTaskWrapperStringAfter = new ObjectMapper().writeValueAsString(eduTaskWrapperObject);
        replyObject.put(EDU_TASK, eduTaskWrapperStringAfter);
        return replyObject;
      }
      catch (IOException e) {
        LOG.error(e);
      }
      return null;
    });
  }

  private void doMigrationTest(@Nullable String afterFileName,
                               @NotNull Function<ObjectNode, ObjectNode> migrationAction) throws IOException {
    String responseString = loadJsonText();
    String afterExpected;
    if (afterFileName != null) {
      afterExpected = loadJsonText(afterFileName);
    } else {
      afterExpected = loadJsonText(getTestName(true) + ".after.json");
    }

    final ObjectNode jsonBefore = (ObjectNode)new ObjectMapper().readTree(responseString);
    ObjectNode jsonAfter = migrationAction.apply(jsonBefore);
    String afterActual = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(jsonAfter);
    afterActual = StringUtilRt.convertLineSeparators(afterActual).replaceAll("\\n\\n", "\n");
    assertEquals(afterExpected, afterActual);
  }

  @NotNull
  private String getTestFile() {
    return getTestName(true) + ".json";
  }
}
