package com.jetbrains.edu.learning.format;

import com.intellij.openapi.util.Pair;
import com.intellij.testFramework.LightPlatformTestCase;
import com.intellij.util.containers.ContainerUtil;
import com.jetbrains.edu.learning.*;
import com.jetbrains.edu.learning.courseFormat.*;
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceOption;
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceOptionStatus;
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class CourseFormatTest extends EduTestCase {

  public void testAdditionalMaterialsLesson() throws IOException {
    final Course course = getCourseFromJson();
    assertNotNull(course.getAdditionalFiles());
    assertFalse(course.getAdditionalFiles().isEmpty());
    assertEquals("test_helper.py", course.getAdditionalFiles().get(0).getName());
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
    assertEquals("First task description", EduUtils.getTaskTextFromTask(getProject(), eduTask));
  }

  public void testDescriptionWithPlaceholderHints() throws IOException {
    EduTask eduTask = getFirstEduTask();
    assertEquals("First task description\n<div class='hint'>my first hint</div>\n\n", EduUtils.getTaskTextFromTask(getProject(), eduTask));
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
    course.init(null, null, false);
    // BACKCOMPAT: 2019.1. Use `getModule()` instead of `myFixture.getModule()`
    CourseBuilderKt.createCourseFiles(course, getProject(), myFixture.getModule(), LightPlatformTestCase.getSourceRoot(), new Object());
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

  public void testChoiceTasks() throws IOException {
    final Course course = getCourseFromJson();
    Task task = course.getLessons().get(0).getTaskList().get(0);
    assertTrue(task instanceof ChoiceTask);
    ChoiceTask choiceTask = (ChoiceTask)task;
    assertTrue(choiceTask.isMultipleChoice());
    List<ChoiceOption> choiceOptions = choiceTask.getChoiceOptions();
    Map<String, ChoiceOptionStatus> actualChoiceOptions =
      ContainerUtil.newHashMap(ContainerUtil.map(choiceOptions, t -> t.getText()), ContainerUtil.map(choiceOptions, t -> t.getStatus()));
    assertEquals(ContainerUtil.newHashMap(Pair.create("1", ChoiceOptionStatus.CORRECT), Pair.create("2", ChoiceOptionStatus.INCORRECT)), actualChoiceOptions);
  }

  public void testCourseWithAuthors() throws IOException {
    final Course course = getCourseFromJson();
    assertEquals(ContainerUtil.newArrayList("EduTools Dev", "EduTools QA", "EduTools"),
                 ContainerUtil.map(course.getAuthors(), info -> info.getName()));
  }

  public void testSolutionsHidden() throws IOException {
    final Course course = getCourseFromJson();
    assertTrue(course.getSolutionsHidden());
  }

  private Course getCourseFromJson() throws IOException {
    final String fileName = getTestFile();
    return CourseTestUtilsKt.createCourseFromJson(getTestDataPath() + fileName, CourseMode.STUDENT);
  }

  @NotNull
  protected String getTestDataPath() {
    return super.getTestDataPath() + "/format/";
  }

  @NotNull
  private String getTestFile() {
    return getTestName(true) + ".json";
  }
}
