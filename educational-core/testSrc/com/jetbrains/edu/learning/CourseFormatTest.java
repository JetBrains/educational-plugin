package com.jetbrains.edu.learning;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.intellij.openapi.util.io.FileUtil;
import com.jetbrains.edu.learning.courseFormat.*;
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.courseFormat.tasks.TaskWithSubtasks;
import com.jetbrains.edu.learning.serialization.SerializationUtils;
import com.jetbrains.edu.learning.stepik.StepikNames;
import org.jetbrains.annotations.NotNull;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static junit.framework.TestCase.*;
import static org.junit.Assert.assertNotNull;


public class CourseFormatTest {
  @Rule
  public TestName name = new TestName();

  @Test
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

  @Test
  public void testPycharmToEduTask() throws IOException {
    final Course course = getCourseFromJson();
    final List<Lesson> lessons = course.getLessons();
    assertFalse("No lessons found", lessons.isEmpty());
    final Lesson lesson = lessons.get(0);
    final List<Task> taskList = lesson.getTaskList();
    assertFalse("No tasks found", taskList.isEmpty());
    assertTrue(taskList.get(0) instanceof EduTask);
  }

  @Test
  public void testSubtask() throws IOException {
    final Course course = getCourseFromJson();
    final List<Lesson> lessons = course.getLessons();
    assertFalse("No lessons found", lessons.isEmpty());
    final Lesson lesson = lessons.get(0);
    final List<Task> taskList = lesson.getTaskList();
    assertFalse("No tasks found", taskList.isEmpty());
    final Task task = taskList.get(0);
    assertTrue(task instanceof TaskWithSubtasks);
    TaskWithSubtasks taskWithSubtasks = (TaskWithSubtasks) task;
    assertEquals(1, taskWithSubtasks.getLastSubtaskIndex());
  }

  @Test
  public void testDescription() throws IOException {
    final Course course = getCourseFromJson();
    final List<Lesson> lessons = course.getLessons();
    assertFalse("No lessons found", lessons.isEmpty());
    final Lesson lesson = lessons.get(0);
    final List<Task> taskList = lesson.getTaskList();
    assertFalse("No tasks found", taskList.isEmpty());
    final Task task = taskList.get(0);
    assertTrue(task instanceof EduTask);
    EduTask eduTask = (EduTask) task;

    assertEquals("First task description", eduTask.getTaskDescription(false, null));
  }

  @Test
  public void testHint() throws IOException {
    final Course course = getCourseFromJson();
    final List<Lesson> lessons = course.getLessons();
    assertFalse("No lessons found", lessons.isEmpty());
    final Lesson lesson = lessons.get(0);
    final List<Task> taskList = lesson.getTaskList();
    assertFalse("No tasks found", taskList.isEmpty());
    final Task task = taskList.get(0);
    assertTrue(task instanceof EduTask);
    EduTask eduTask = (EduTask) task;

    final TaskFile taskFile = eduTask.getTaskFile("task.py");
    assertNotNull(taskFile);
    final List<AnswerPlaceholder> answerPlaceholders = taskFile.getAnswerPlaceholders();
    assertEquals(1, answerPlaceholders.size());
    final List<String> hints = answerPlaceholders.get(0).getHints();
    assertEquals(1, hints.size());
    assertEquals("my first hint", hints.get(0));
  }

  @Test
  public void testPlaceholderText() throws IOException {
    final Course course = getCourseFromJson();
    final List<Lesson> lessons = course.getLessons();
    assertFalse("No lessons found", lessons.isEmpty());
    final Lesson lesson = lessons.get(0);
    final List<Task> taskList = lesson.getTaskList();
    assertFalse("No tasks found", taskList.isEmpty());
    final Task task = taskList.get(0);
    assertTrue(task instanceof EduTask);
    EduTask eduTask = (EduTask) task;

    final TaskFile taskFile = eduTask.getTaskFile("task.py");
    assertNotNull(taskFile);
    final List<AnswerPlaceholder> answerPlaceholders = taskFile.getAnswerPlaceholders();
    assertEquals(1, answerPlaceholders.size());
    assertEquals("write function body", answerPlaceholders.get(0).getTaskText());
  }

  @Test
  public void testPossibleAnswer() throws IOException {
    final Course course = getCourseFromJson();
    final List<Lesson> lessons = course.getLessons();
    assertFalse("No lessons found", lessons.isEmpty());
    final Lesson lesson = lessons.get(0);
    final List<Task> taskList = lesson.getTaskList();
    assertFalse("No tasks found", taskList.isEmpty());
    final Task task = taskList.get(0);
    assertTrue(task instanceof EduTask);
    EduTask eduTask = (EduTask) task;

    final TaskFile taskFile = eduTask.getTaskFile("task.py");
    assertNotNull(taskFile);
    final List<AnswerPlaceholder> answerPlaceholders = taskFile.getAnswerPlaceholders();
    assertEquals(1, answerPlaceholders.size());
    assertEquals("pass", answerPlaceholders.get(0).getPossibleAnswer());
  }

  @Test
  public void testCourseName() throws IOException {
    final Course course = getCourseFromJson();
    assertEquals("My Python Course", course.getName());
  }

  @Test
  public void testCourseProgrammingLanguage() throws IOException {
    final Course course = getCourseFromJson();
    assertEquals(EduNames.PYTHON, course.getLanguageID());
  }

  @Test
  public void testCourseLanguage() throws IOException {
    final Course course = getCourseFromJson();
    assertEquals("Russian", course.getHumanLanguage());
  }

  @Test
  public void testCourseDescription() throws IOException {
    final Course course = getCourseFromJson();
    assertEquals("Best course ever", course.getDescription());
  }

  @Test
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

  @Test
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

  @Test
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
    assertEquals("def foo():\n    write function body\n", taskFile.text);
  }

  private Course getCourseFromJson() throws IOException {
    final String fileName = getTestFile();
    String courseJson = FileUtil.loadFile(new File(getTestDataPath(), fileName));

    Gson gson = new GsonBuilder()
        .registerTypeAdapter(Task.class, new SerializationUtils.Json.TaskAdapter())
        .registerTypeAdapter(StudyItem.class, new SerializationUtils.Json.LessonSectionAdapter())
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .create();
    return gson.fromJson(courseJson, Course.class);
  }

  @NotNull
  private static String getTestDataPath() {
    return FileUtil.join("testData/format");
  }

  @NotNull
  private String getTestFile() {
    final String methodName = name.getMethodName();
    String fileName = methodName.substring("test".length());
    fileName = Character.toLowerCase(fileName.charAt(0)) + fileName.substring(1);
    return fileName + ".json";
  }
}
