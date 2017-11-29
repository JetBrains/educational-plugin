package com.jetbrains.edu.learning.actions;

import com.intellij.testFramework.TestActionEvent;
import com.jetbrains.edu.learning.EduTestCase;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.Lesson;

import java.io.IOException;

public class RefreshPlaceholderTest extends EduTestCase {

  public void testRefreshPlaceholder() {
    configureByTaskFile(1, 1, "taskFile1.txt");
    myFixture.getEditor().getCaretModel().moveToOffset(12);

    myFixture.type("test");
    myFixture.testAction(new RefreshAnswerPlaceholder());

    assertEquals("Look! There is placeholder.",
        myFixture.getDocument(myFixture.getFile()).getText());
  }

  public void testCaretOutside() {
    configureByTaskFile(1, 2, "taskFile2.txt");
    myFixture.getEditor().getCaretModel().moveToOffset(2);
    myFixture.type("test");
    myFixture.getEditor().getCaretModel().moveToOffset(2);

    final RefreshAnswerPlaceholder action = new RefreshAnswerPlaceholder();
    TestActionEvent e = new TestActionEvent(action);
    action.beforeActionPerformedUpdate(e);
    assertFalse(e.getPresentation().isEnabled() && e.getPresentation().isVisible());
  }

  public void testSecondRefreshPlaceholder() {
    configureByTaskFile(1, 3, "taskFile3.txt");
    myFixture.getEditor().getCaretModel().moveToOffset(16);

    myFixture.type("test");
    myFixture.getEditor().getCaretModel().moveToOffset(52);
    myFixture.type("test");
    myFixture.testAction(new RefreshAnswerPlaceholder());

    assertEquals("Look! There is test placeholder.\n" +
            "Look! There is second placeholder.",
        myFixture.getDocument(myFixture.getFile()).getText());
  }

  public void testFirstRefreshPlaceholder() {
    configureByTaskFile(1, 3, "taskFile3.txt");
    myFixture.getEditor().getCaretModel().moveToOffset(16);

    myFixture.type("test");
    myFixture.getEditor().getCaretModel().moveToOffset(52);
    myFixture.type("test");
    myFixture.getEditor().getCaretModel().moveToOffset(16);
    myFixture.testAction(new RefreshAnswerPlaceholder());
    assertEquals("Look! There is first placeholder.\n" +
            "Look! There is secotestnd placeholder.",
        myFixture.getDocument(myFixture.getFile()).getText());
  }

  @Override
  protected void createCourse() throws IOException {
    myFixture.copyDirectoryToProject("lesson1", "lesson1");
    Course course = new Course();
    course.setName("Edu test course");
    course.setLanguage("JAVA");
    StudyTaskManager.getInstance(myFixture.getProject()).setCourse(course);

    Lesson lesson1 = createLesson(1, 3);
    course.addLesson(lesson1);
    course.initCourse(false);
  }

  @Override
  protected String getTestDataPath() {
    return super.getTestDataPath() + "/actions/refreshPlaceholder";
  }
}
