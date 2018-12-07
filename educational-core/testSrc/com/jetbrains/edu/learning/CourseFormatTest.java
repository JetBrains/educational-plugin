package com.jetbrains.edu.learning;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.intellij.openapi.util.io.FileUtil;
import com.jetbrains.edu.learning.courseFormat.*;
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.serialization.SerializationUtils;
import com.jetbrains.edu.learning.stepik.StepikNames;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;


public class CourseFormatTest extends EduTestCase {
  public void testAdditionalMaterialsLesson() throws IOException {
    final Course course = getCourseFromJson();
    final List<Lesson> lessons = course.getLessons(true);
    final Lesson additional = lessons.stream().
        filter(lesson -> lesson.getName().equals(EduNames.ADDITIONAL_MATERIALS)).findFirst().orElse(null);
    final Lesson oldAdditional = lessons.stream().
        filter(lesson -> lesson.getName().equals(StepikNames.PYCHARM_ADDITIONAL)).findFirst().orElse(null);
    assertNotNull(additional);
    assertNull(oldAdditional);
  }

  public void testCourseWithSection() throws IOException {
    final Course course = getCourseFromJson();
    final List<StudyItem> items = course.getItems();
    assertEquals(2, items.size());
    assertTrue(items.get(0) instanceof Section);
    assertTrue(items.get(1) instanceof Lesson);
    assertEquals(1, ((Section)items.get(0)).getLessons().size());
  }

  public void testFrameworkLesson() throws IOException {
    final Course course = getCourseFromJson();
    final List<StudyItem> items = course.getItems();
    assertEquals(1, items.size());
    assertTrue(items.get(0) instanceof FrameworkLesson);
  }

  public void testPycharmToEduTask() throws IOException {
    final Course course = getCourseFromJson();
    final List<Lesson> lessons = course.getLessons();
    assertFalse("No lessons found", lessons.isEmpty());
    final Lesson lesson = lessons.get(0);
    final List<Task> taskList = lesson.getTaskList();
    assertFalse("No tasks found", taskList.isEmpty());
    assertTrue(taskList.get(0) instanceof EduTask);
  }

  public void testDescription() throws IOException {
    EduTask eduTask = getFirstEduTask();
    assertEquals("First task description", eduTask.getTaskDescription(false, null));
  }

  public void testFeedbackLinks() throws IOException {
    EduTask eduTask = getFirstEduTask();

    FeedbackLink feedbackLink = eduTask.getFeedbackLink();
    assertEquals(FeedbackLink.LinkType.CUSTOM, feedbackLink.getType());
    assertEquals("https://www.jetbrains.com/", feedbackLink.getLink());
  }

  @NotNull
  private EduTask getFirstEduTask() throws IOException {
    final Course course = getCourseFromJson();
    final List<Lesson> lessons = course.getLessons();
    assertFalse("No lessons found", lessons.isEmpty());
    final Lesson lesson = lessons.get(0);
    final List<Task> taskList = lesson.getTaskList();
    assertFalse("No tasks found", taskList.isEmpty());
    final Task task = taskList.get(0);
    assertTrue(task instanceof EduTask);
    return (EduTask)task;
  }

  public void testHint() throws IOException {
    EduTask eduTask = getFirstEduTask();
    final TaskFile taskFile = eduTask.getTaskFile("task.py");
    assertNotNull(taskFile);
    final List<AnswerPlaceholder> answerPlaceholders = taskFile.getAnswerPlaceholders();
    assertEquals(1, answerPlaceholders.size());
    final List<String> hints = answerPlaceholders.get(0).getHints();
    assertEquals(1, hints.size());
    assertEquals("my first hint", hints.get(0));
  }

  public void testPlaceholderText() throws IOException {
    EduTask eduTask = getFirstEduTask();
    final TaskFile taskFile = eduTask.getTaskFile("task.py");
    assertNotNull(taskFile);
    final List<AnswerPlaceholder> answerPlaceholders = taskFile.getAnswerPlaceholders();
    assertEquals(1, answerPlaceholders.size());
    assertEquals("write function body", answerPlaceholders.get(0).getPlaceholderText());
  }

  public void testPossibleAnswer() throws IOException {
    EduTask eduTask = getFirstEduTask();
    final TaskFile taskFile = eduTask.getTaskFile("task.py");
    assertNotNull(taskFile);
    final List<AnswerPlaceholder> answerPlaceholders = taskFile.getAnswerPlaceholders();
    assertEquals(1, answerPlaceholders.size());
    assertEquals("pass", answerPlaceholders.get(0).getPossibleAnswer());
  }

  public void testCourseName() throws IOException {
    final Course course = getCourseFromJson();
    assertEquals("My Python Course", course.getName());
  }

  public void testCourseProgrammingLanguage() throws IOException {
    final Course course = getCourseFromJson();
    assertEquals(EduNames.PYTHON, course.getLanguageID());
  }

  public void testCourseLanguage() throws IOException {
    final Course course = getCourseFromJson();
    assertEquals("Russian", course.getHumanLanguage());
  }

  public void testCourseDescription() throws IOException {
    final Course course = getCourseFromJson();
    assertEquals("Best course ever", course.getDescription());
  }

  public void testTestFiles() throws IOException {
    final Course course = getCourseFromJson();
    final List<Lesson> lessons = course.getLessons();
    assertFalse("No lessons found", lessons.isEmpty());
    final Lesson lesson = lessons.get(0);
    final List<Task> taskList = lesson.getTaskList();
    assertFalse("No tasks found", taskList.isEmpty());
    final Task task = taskList.get(0);
    assertEquals(1, task.getTestsText().size());
  }

  public void testTestFilesCustomName() throws IOException {
    final Course course = getCourseFromJson();
    final List<Lesson> lessons = course.getLessons();
    assertFalse("No lessons found", lessons.isEmpty());
    final Lesson lesson = lessons.get(0);
    final List<Task> taskList = lesson.getTaskList();
    assertFalse("No tasks found", taskList.isEmpty());
    final Task task = taskList.get(0);
    final Map<String, String> testsText = task.getTestsText();
    assertEquals(2, testsText.size());
    assertTrue(testsText.containsKey("super_test.py"));
  }

  public void testStudentTaskText() throws IOException {
    final Course course = getCourseFromJson();
    final List<Lesson> lessons = course.getLessons();
    assertFalse("No lessons found", lessons.isEmpty());
    final Lesson lesson = lessons.get(0);
    final List<Task> taskList = lesson.getTaskList();
    assertFalse("No tasks found", taskList.isEmpty());
    final Task task = taskList.get(0);
    final TaskFile taskFile = task.getTaskFile("my_task.py");
    assertNotNull(taskFile);
    assertEquals("def foo():\n    write function body\n", taskFile.getText());
  }

  private Course getCourseFromJson() throws IOException {
    final String fileName = getTestFile();
    String courseJson = FileUtil.loadFile(new File(getTestDataPath(), fileName));

    Gson gson = new GsonBuilder()
        .registerTypeAdapter(Task.class, new SerializationUtils.Json.TaskAdapter())
        .registerTypeAdapter(StudyItem.class, new SerializationUtils.Json.LessonSectionAdapter())
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .create();
    return gson.fromJson(courseJson, EduCourse.class);
  }

  @NotNull
  protected String getTestDataPath() {
    return super.getTestDataPath() + "/format";
  }

  @NotNull
  private String getTestFile() {
    return getTestName(true) + ".json";
  }
}
