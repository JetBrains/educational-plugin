package com.jetbrains.edu.learning;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.intellij.openapi.util.io.FileUtil;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.Lesson;
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.courseFormat.tasks.TaskWithSubtasks;
import com.jetbrains.edu.learning.stepic.StepicNames;
import org.jetbrains.annotations.NotNull;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import java.io.File;
import java.io.IOException;
import java.util.List;

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
        filter(lesson -> lesson.getName().equals(StepicNames.PYCHARM_ADDITIONAL)).findFirst().orElse(null);
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

  private Course getCourseFromJson() throws IOException {
    final String fileName = getTestFile();
    String courseJson = FileUtil.loadFile(new File(getTestDataPath(), fileName));

    Gson gson = new GsonBuilder()
        .registerTypeAdapter(Task.class, new SerializationUtils.Json.TaskAdapter())
        .registerTypeAdapter(Lesson.class, new SerializationUtils.Json.LessonAdapter())
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
