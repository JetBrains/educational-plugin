package com.jetbrains.edu.learning;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.intellij.openapi.util.io.FileUtil;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.Lesson;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.stepic.StepicNames;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertNotNull;


public class CourseFormatTest {
  @Test
  public void testAdditionalMaterialsLesson() throws IOException {
    final Course course = getCourseFromJson("course.json");
    final List<Lesson> lessons = course.getLessons(true);
    final Lesson additional = lessons.stream().
        filter(lesson -> lesson.getName().equals(EduNames.ADDITIONAL_MATERIALS)).findFirst().orElse(null);
    final Lesson oldAdditional = lessons.stream().
        filter(lesson -> lesson.getName().equals(StepicNames.PYCHARM_ADDITIONAL)).findFirst().orElse(null);
    assertNotNull(additional);
    assertNull(oldAdditional);
  }

  private static Course getCourseFromJson(@NotNull final String fileName) throws IOException {
    String courseJson =
        FileUtil.loadFile(new File(getTestDataPath(), fileName));

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
}
