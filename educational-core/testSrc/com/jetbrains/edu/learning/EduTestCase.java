package com.jetbrains.edu.learning;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase;
import com.jetbrains.edu.coursecreator.CCTestCase;
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder;
import com.jetbrains.edu.learning.courseFormat.Lesson;
import com.jetbrains.edu.learning.courseFormat.TaskFile;
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public abstract class EduTestCase extends LightPlatformCodeInsightFixtureTestCase {
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    createCourse();
  }

  protected void createCourse() throws IOException {}

  @NotNull
  protected Lesson createLesson(int index, int taskCount) throws IOException {
    Lesson lesson = new Lesson();
    lesson.setName("lesson" + index);
    for (int i = 1; i <= taskCount; ++i) {
      Task task = createTask(index, i);
      lesson.addTask(task);
    }
    lesson.setIndex(index);
    return lesson;
  }

  @NotNull
  protected Task createTask(int lessonIndex, int taskIndex) throws IOException {
    Task task = new EduTask();
    task.setName("task" + taskIndex);
    task.setIndex(taskIndex);
    createTaskFile(lessonIndex, task, "taskFile" + taskIndex + ".txt");
    return task;
  }

  private void createTaskFile(int lessonIndex, Task task, String taskFilePath) throws IOException {
    TaskFile taskFile = new TaskFile();
    taskFile.setTask(task);
    task.getTaskFiles().put(taskFilePath, taskFile);
    taskFile.name = taskFilePath;

    final String fileName = "lesson" + lessonIndex + "/" + task.getName() + "/" + taskFilePath;
    final VirtualFile file = myFixture.findFileInTempDir(fileName);
    taskFile.text = VfsUtilCore.loadText(file);

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


