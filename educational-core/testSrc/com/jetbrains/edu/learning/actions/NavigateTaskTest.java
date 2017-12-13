package com.jetbrains.edu.learning.actions;

import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.edu.learning.EduNames;
import com.jetbrains.edu.learning.EduTestCase;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.Lesson;
import com.jetbrains.edu.learning.courseFormat.TaskFile;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;

import java.io.IOException;

public class NavigateTaskTest extends EduTestCase {

  public void testNextTask() {
    configureByTaskFile(1, 1, "taskFile1.txt");
    myFixture.testAction(new NextTaskAction());
    final VirtualFile currentFile = FileEditorManagerEx.getInstanceEx(myFixture.getProject()).getCurrentFile();
    final TaskFile taskFile = EduUtils.getTaskFile(myFixture.getProject(), currentFile);
    final Task task = taskFile.getTask();
    assertEquals(2, task.getIndex());
  }

  public void testPreviousTask() {
    configureByTaskFile(1, 2, "taskFile2.txt");
    myFixture.testAction(new PreviousTaskAction());
    final VirtualFile currentFile = FileEditorManagerEx.getInstanceEx(myFixture.getProject()).getCurrentFile();
    final TaskFile taskFile = EduUtils.getTaskFile(myFixture.getProject(), currentFile);
    final Task task = taskFile.getTask();
    assertEquals(1, task.getIndex());
  }

  public void testNextLesson() {
    configureByTaskFile(1, 2, "taskFile2.txt");
    myFixture.testAction(new NextTaskAction());
    final VirtualFile currentFile = FileEditorManagerEx.getInstanceEx(myFixture.getProject()).getCurrentFile();
    final TaskFile taskFile = EduUtils.getTaskFile(myFixture.getProject(), currentFile);
    final Task task = taskFile.getTask();
    assertEquals(1, task.getIndex());
    final Lesson lesson = task.getLesson();
    assertEquals(2, lesson.getIndex());
  }

  public void testPreviousLesson() {
    configureByTaskFile(2, 1, "taskFile1.txt");
    myFixture.testAction(new PreviousTaskAction());
    final VirtualFile currentFile = FileEditorManagerEx.getInstanceEx(myFixture.getProject()).getCurrentFile();
    final TaskFile taskFile = EduUtils.getTaskFile(myFixture.getProject(), currentFile);
    final Task task = taskFile.getTask();
    assertEquals(2, task.getIndex());
    final Lesson lesson = task.getLesson();
    assertEquals(1, lesson.getIndex());
  }

  public void testLastTask() {
    configureByTaskFile(2, 2, "taskFile2.txt");
    myFixture.testAction(new NextTaskAction());
    final VirtualFile currentFile = FileEditorManagerEx.getInstanceEx(myFixture.getProject()).getCurrentFile();
    final TaskFile taskFile = EduUtils.getTaskFile(myFixture.getProject(), currentFile);
    final Task task = taskFile.getTask();
    assertEquals(2, task.getIndex());
    final Lesson lesson = task.getLesson();
    assertEquals(2, lesson.getIndex());
  }

  public void testFirstTask() {
    configureByTaskFile(1, 1, "taskFile1.txt");
    myFixture.testAction(new PreviousTaskAction());
    final VirtualFile currentFile = FileEditorManagerEx.getInstanceEx(myFixture.getProject()).getCurrentFile();
    final TaskFile taskFile = EduUtils.getTaskFile(myFixture.getProject(), currentFile);
    final Task task = taskFile.getTask();
    assertEquals(1, task.getIndex());
    final Lesson lesson = task.getLesson();
    assertEquals(1, lesson.getIndex());
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
    Lesson lesson2 = createLesson(2, 2);
    course.addLesson(lesson1);
    course.addLesson(lesson2);
    course.initCourse(false);
  }

  @Override
  protected String getTestDataPath() {
    return super.getTestDataPath() + "/actions/navigate";
  }
}
