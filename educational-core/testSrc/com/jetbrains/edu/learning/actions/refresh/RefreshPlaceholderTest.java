package com.jetbrains.edu.learning.actions.refresh;

import com.intellij.openapi.fileTypes.PlainTextLanguage;
import com.intellij.testFramework.TestActionEvent;
import com.jetbrains.edu.learning.EduTestCase;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.actions.RefreshAnswerPlaceholder;
import com.jetbrains.edu.learning.courseFormat.*;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;

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

  public void testRefreshSecondPlaceholderStartOffset() {
    configureByTaskFile(1, 3, "taskFile3.txt");
    myFixture.getEditor().getCaretModel().moveToOffset(16);

    myFixture.type("test test");
    myFixture.getEditor().getCaretModel().moveToOffset(56);
    myFixture.type("test");
    myFixture.testAction(new RefreshAnswerPlaceholder());

    assertEquals("Look! There is test test placeholder.\n" +
            "Look! There is second placeholder.",
        myFixture.getDocument(myFixture.getFile()).getText());
    final Course course = StudyTaskManager.getInstance(getProject()).getCourse();
    final Lesson lesson = course.getLesson("lesson1");
    final Task task = lesson.getTask("task3");
    final TaskFile taskFile = task.getTaskFile("taskFile3.txt");
    final List<AnswerPlaceholder> placeholders = taskFile.getAnswerPlaceholders();
    assertEquals(2, placeholders.size());
    final AnswerPlaceholder secondPlaceholder = placeholders.get(1);
    assertEquals(53, secondPlaceholder.getOffset());
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
    Course course = new EduCourse();
    course.setName("Edu test course");
    course.setLanguage(PlainTextLanguage.INSTANCE.getID());
    StudyTaskManager.getInstance(myFixture.getProject()).setCourse(course);

    Lesson lesson1 = createLesson(1, 3);
    course.addLesson(lesson1);
    course.init(null, null, false);
  }

  @NotNull
  @Override
  protected String getTestDataPath() {
    return super.getTestDataPath() + "/actions/refreshPlaceholder";
  }
}
