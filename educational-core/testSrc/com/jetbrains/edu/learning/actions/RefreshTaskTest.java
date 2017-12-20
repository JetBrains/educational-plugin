package com.jetbrains.edu.learning.actions;

import com.intellij.openapi.fileTypes.PlainTextLanguage;
import com.jetbrains.edu.learning.EduTestCase;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.Lesson;

import java.io.IOException;

public class RefreshTaskTest extends EduTestCase {

  public void testRefreshTask() {
    configureByTaskFile(1, 1, "taskFile1.txt");
    myFixture.type("test");
    myFixture.testAction(new RefreshTaskFileAction());

    assertEquals("Look! There <placeholder hint=\"my hint\">i<caret>s</placeholder> placeholder.",
        myFixture.getDocument(myFixture.getFile()).getText());
  }

  public void testCaretOutside() {
    configureByTaskFile(1, 2, "taskFile2.txt");
    myFixture.type("test");
    myFixture.testAction(new RefreshTaskFileAction());

    assertEquals("Look!<caret> There <placeholder hint=\"my hint\">is</placeholder> placeholder.",
        myFixture.getDocument(myFixture.getFile()).getText());
  }

  @Override
  protected void createCourse() throws IOException {
    myFixture.copyDirectoryToProject("lesson1", "lesson1");
    Course course = new Course();
    course.setName("Edu test course");
    course.setLanguage(PlainTextLanguage.INSTANCE.getID());
    StudyTaskManager.getInstance(myFixture.getProject()).setCourse(course);

    Lesson lesson1 = createLesson(1, 2);
    course.addLesson(lesson1);
    course.initCourse(false);
  }

  @Override
  protected String getTestDataPath() {
    return super.getTestDataPath() + "/actions/refresh";
  }
}
