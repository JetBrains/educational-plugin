package com.jetbrains.edu.learning;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase;
import com.jetbrains.edu.coursecreator.CCTestCase;
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder;
import com.jetbrains.edu.learning.courseFormat.Lesson;
import com.jetbrains.edu.learning.courseFormat.TaskFile;
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import org.jetbrains.annotations.NotNull;

public abstract class EduTestCase extends LightPlatformCodeInsightFixtureTestCase {
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    createCourse();
  }

  protected void createCourse() {}

  @NotNull
  protected Lesson createLesson(int index) {
    Lesson lesson = new Lesson();
    lesson.setName("lesson" + index);
    Task task1 = createTask(index, 1);
    Task task2 = createTask(index, 2);
    lesson.addTask(task1);
    lesson.addTask(task2);

    lesson.setIndex(index);
    return lesson;
  }

  @NotNull
  private Task createTask(int lessonIndex, int taskIndex) {
    Task task = new EduTask();
    task.setName("task" + taskIndex);
    task.setIndex(taskIndex);
    createTaskFile(lessonIndex, task, "taskFile" + taskIndex + ".txt");
    return task;
  }

  private void createTaskFile(int lessonIndex, Task task, String taskFilePath) {
    TaskFile taskFile = new TaskFile();
    taskFile.setTask(task);
    task.getTaskFiles().put(taskFilePath, taskFile);
    taskFile.name = taskFilePath;

    final String fileName = "lesson" + lessonIndex + "/" + task.getName() + "/" + taskFilePath;
    final VirtualFile file = myFixture.findFileInTempDir(fileName);
    myFixture.configureFromExistingVirtualFile(file);
    Document document = FileDocumentManager.getInstance().getDocument(file);
    for (AnswerPlaceholder placeholder : CCTestCase.getPlaceholders(document, false)) {
      taskFile.addAnswerPlaceholder(placeholder);
    }
    taskFile.sortAnswerPlaceholders();
    EduUtils.drawAllAnswerPlaceholders(myFixture.getEditor(), taskFile);
  }

  protected void configureByTaskFile(int lessonIndex, int taskIndex, String taskFileName) {
    final String fileName = "lesson" + lessonIndex + "/task" + taskIndex + "/" + taskFileName;
    final VirtualFile file = myFixture.findFileInTempDir(fileName);
    myFixture.configureFromExistingVirtualFile(file);
  }

  protected String getTestDataPath() {
    return "testData";
  }
}


