package com.jetbrains.edu.learning.actions;

import com.intellij.openapi.fileTypes.PlainTextLanguage;
import com.jetbrains.edu.learning.EduState;
import com.jetbrains.edu.learning.EduTestCase;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.Lesson;
import com.jetbrains.edu.learning.ui.AnswerPlaceholderHint;
import com.jetbrains.edu.learning.ui.HintComponent;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class ShowHintTest extends EduTestCase {
  public void testOneHint() {
    configureByTaskFile(1, 1, "taskFile1.txt");
    EduState eduState = new EduState(EduUtils.getSelectedEduEditor(myFixture.getProject()));
    int offset = myFixture.getCaretOffset();
    AnswerPlaceholder answerPlaceholder = eduState.getTaskFile().getAnswerPlaceholder(offset);

    final AnswerPlaceholderHint hint = ShowHintAction.getHint(myFixture.getProject(), answerPlaceholder);
    final HintComponent hintContent = hint.getHintComponent();
    final String text = hintContent.getText();
    assertEquals("my hint", text.trim());
  }

  public void testOutsidePlaceholder() {
    configureByTaskFile(1, 2, "taskFile2.txt");
    EduState eduState = new EduState(EduUtils.getSelectedEduEditor(myFixture.getProject()));
    AnswerPlaceholder answerPlaceholder = eduState.getTaskFile().getAnswerPlaceholder(5);

    final AnswerPlaceholderHint hint = ShowHintAction.getHint(myFixture.getProject(), answerPlaceholder);
    final HintComponent hintContent = hint.getHintComponent();
    final String text = hintContent.getText();
    assertEquals(AnswerPlaceholderHint.Companion.getNO_PLACEHOLDER_MESSAGE(), text.trim());
  }

  public void testSecondHints() {
    configureByTaskFile(1, 3, "taskFile3.txt");
    EduState eduState = new EduState(EduUtils.getSelectedEduEditor(myFixture.getProject()));
    int offset = myFixture.getCaretOffset();
    AnswerPlaceholder answerPlaceholder = eduState.getTaskFile().getAnswerPlaceholder(offset);

    final AnswerPlaceholderHint hint = ShowHintAction.getHint(myFixture.getProject(), answerPlaceholder);
    final AnswerPlaceholderHint.GoForward goForward = hint.getGoForwardAction();
    myFixture.testAction(goForward);
    final HintComponent hintContent = hint.getHintComponent();
    final String text = hintContent.getText();
    assertEquals("my hint2", text.trim());
  }

  public void testGoBackwardHints() {
    configureByTaskFile(1, 3, "taskFile3.txt");
    EduState eduState = new EduState(EduUtils.getSelectedEduEditor(myFixture.getProject()));
    int offset = myFixture.getCaretOffset();
    AnswerPlaceholder answerPlaceholder = eduState.getTaskFile().getAnswerPlaceholder(offset);

    final AnswerPlaceholderHint hint = ShowHintAction.getHint(myFixture.getProject(), answerPlaceholder);
    final AnswerPlaceholderHint.GoForward goForward = hint.getGoForwardAction();
    myFixture.testAction(goForward);
    myFixture.testAction(hint.getGoBackwardAction());
    final HintComponent hintContent = hint.getHintComponent();
    final String text = hintContent.getText();
    assertEquals("my hint1", text.trim());
  }

  public void testNoHints() {
    configureByTaskFile(1, 4, "taskFile4.txt");
    EduState eduState = new EduState(EduUtils.getSelectedEduEditor(myFixture.getProject()));
    int offset = myFixture.getCaretOffset();
    AnswerPlaceholder answerPlaceholder = eduState.getTaskFile().getAnswerPlaceholder(offset);

    final AnswerPlaceholderHint hint = ShowHintAction.getHint(myFixture.getProject(), answerPlaceholder);
    final HintComponent hintContent = hint.getHintComponent();
    final String text = hintContent.getText();
    assertEquals(AnswerPlaceholderHint.Companion.getHINTS_NOT_AVAILABLE(), text.trim());
  }

  @Override
  protected void createCourse() throws IOException {
    myFixture.copyDirectoryToProject("lesson1", "lesson1");
    Course course = new Course();
    course.setLanguage(PlainTextLanguage.INSTANCE.getID());
    course.setName("Edu test course");
    StudyTaskManager.getInstance(myFixture.getProject()).setCourse(course);

    Lesson lesson1 = createLesson(1, 4);
    course.addLesson(lesson1);
    course.init(null, null, false);
  }

  @NotNull
  @Override
  protected String getTestDataPath() {
    return super.getTestDataPath() + "/actions/showHint";
  }
}
