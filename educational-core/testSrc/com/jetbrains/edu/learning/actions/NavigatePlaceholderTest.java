package com.jetbrains.edu.learning.actions;

import com.jetbrains.edu.learning.EduNames;
import com.jetbrains.edu.learning.EduTestCase;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.Lesson;

import java.io.IOException;

import static org.junit.Assert.assertNotEquals;

public class NavigatePlaceholderTest extends EduTestCase {

  public void testNextPlaceholder() {
    configureByTaskFile(1, 1, "taskFile1.txt");
    final int caretOffset = myFixture.getCaretOffset();
    myFixture.testAction(new NextPlaceholderAction());
    assertNotEquals(caretOffset, myFixture.getCaretOffset());
    assertEquals(44, myFixture.getCaretOffset());
  }

  public void testPreviousPlaceholder() {
    configureByTaskFile(1, 2, "taskFile2.txt");
    final int caretOffset = myFixture.getCaretOffset();
    myFixture.testAction(new PrevPlaceholderAction());
    assertNotEquals(caretOffset, myFixture.getCaretOffset());
    assertEquals(12, myFixture.getCaretOffset());
  }

  public void testOnePlaceholderNext() {
    configureByTaskFile(2, 1, "taskFile1.txt");
    final int caretOffset = myFixture.getCaretOffset();
    myFixture.testAction(new NextPlaceholderAction());
    assertNotEquals(caretOffset, myFixture.getCaretOffset());
    assertEquals(12, myFixture.getCaretOffset());
  }

  public void testOnePlaceholderPrevious() {
    configureByTaskFile(2, 1, "taskFile1.txt");
    final int caretOffset = myFixture.getCaretOffset();
    myFixture.testAction(new PrevPlaceholderAction());
    assertEquals(caretOffset, myFixture.getCaretOffset());
  }

  public void testLastPlaceholderNext() {
    configureByTaskFile(1, 2, "taskFile2.txt");
    final int caretOffset = myFixture.getCaretOffset();
    myFixture.testAction(new NextPlaceholderAction());
    assertNotEquals(caretOffset, myFixture.getCaretOffset());
    assertEquals(12, myFixture.getCaretOffset());
  }

  public void testFirstPlaceholderPrevious() {
    configureByTaskFile(1, 1, "taskFile1.txt");
    final int caretOffset = myFixture.getCaretOffset();
    myFixture.testAction(new NextPlaceholderAction());
    assertNotEquals(caretOffset, myFixture.getCaretOffset());
    assertEquals(44, myFixture.getCaretOffset());
  }

  public void testNotInPlaceholderNext() {
    configureByTaskFile(2, 2, "taskFile2.txt");
    final int caretOffset = myFixture.getCaretOffset();
    myFixture.testAction(new NextPlaceholderAction());
    assertNotEquals(caretOffset, myFixture.getCaretOffset());
    assertEquals(19, myFixture.getCaretOffset());
  }

  public void testNotInPlaceholderPrevious() {
    configureByTaskFile(2, 2, "taskFile2.txt");
    final int caretOffset = myFixture.getCaretOffset();
    myFixture.testAction(new PrevPlaceholderAction());
    assertEquals(caretOffset, myFixture.getCaretOffset());
  }

  public void testCaretAfterPlaceholderNext() {
    configureByTaskFile(2, 3, "taskFile3.txt");
    final int caretOffset = myFixture.getCaretOffset();
    myFixture.testAction(new NextPlaceholderAction());
    assertEquals(caretOffset, myFixture.getCaretOffset());
  }

  public void testCaretAfterPlaceholderPrevious() {
    configureByTaskFile(2, 3, "taskFile3.txt");
    final int caretOffset = myFixture.getCaretOffset();
    myFixture.testAction(new PrevPlaceholderAction());
    assertNotEquals(caretOffset, myFixture.getCaretOffset());
    assertEquals(12, myFixture.getCaretOffset());
  }

  @Override
  protected void createCourse() throws IOException {
    myFixture.copyDirectoryToProject("lesson1", "lesson1");
    myFixture.copyDirectoryToProject("lesson2", "lesson2");
    Course course = new Course();
    course.setName("Edu test course");
    course.setLanguage(EduNames.JAVA);
    StudyTaskManager.getInstance(myFixture.getProject()).setCourse(course);

    Lesson lesson1 = createLesson(1, 2);
    Lesson lesson2 = createLesson(2, 3);
    course.addLesson(lesson1);
    course.addLesson(lesson2);
    course.initCourse(false);
  }

  @Override
  protected String getTestDataPath() {
    return super.getTestDataPath() + "/actions/navigatePlaceholder";
  }
}
