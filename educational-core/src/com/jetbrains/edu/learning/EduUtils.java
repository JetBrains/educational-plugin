package com.jetbrains.edu.learning;

import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ObjectUtils;
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson;
import com.jetbrains.edu.learning.courseFormat.Lesson;
import com.jetbrains.edu.learning.courseFormat.ext.StudyItemExtKt;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse;
import com.jetbrains.edu.learning.taskDescription.TaskDescriptionUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.jetbrains.edu.learning.courseFormat.EduFormatNames.TASK_HTML;
import static com.jetbrains.edu.learning.courseFormat.EduFormatNames.TASK_MD;

public class EduUtils {

  private EduUtils() {
  }

  private static final Logger LOG = Logger.getInstance(EduUtils.class.getName());

  @Nullable
  public static String getTaskTextFromTask(@NotNull final Project project, @Nullable final Task task) {
    if (task == null) {
      return null;
    }
    Lesson lesson = task.getLesson();
    VirtualFile lessonDir = StudyItemExtKt.getDir(lesson, OpenApiExtKt.getCourseDir(project));
    if (lessonDir == null) {
      return null;
    }
    VirtualFile taskDirectory = lesson instanceof FrameworkLesson ? lessonDir.findChild(task.getName())
                                                                  : StudyItemExtKt.getDir(task, OpenApiExtKt.getCourseDir(project));
    String text = getTaskTextByTaskName(task, taskDirectory);
    if (text == null) {
      LOG.warn("Cannot find task description file for a task: " + task.getName());
      return null;
    }
    text = StringUtil.replace(text, "%IDE_NAME%", ApplicationNamesInfo.getInstance().getFullProductName());
    if (lesson instanceof FrameworkLesson) {
      text = TaskDescriptionUtil.addHeader(task, lesson.getTaskList().size(), text);
    }
    StringBuffer textBuffer = new StringBuffer(text);
    TaskDescriptionUtil.replaceActionIDsWithShortcuts(textBuffer);
    if (task.getCourse() instanceof HyperskillCourse) {
      TaskDescriptionUtil.removeHyperskillTags(textBuffer);
    }
    return textBuffer.toString();
  }

  @Nullable
  private static String getTaskTextByTaskName(@NotNull Task task, @Nullable VirtualFile taskDirectory) {
    if (taskDirectory == null) return null;

    VirtualFile taskTextFile = getTaskTextFile(taskDirectory);
    String taskDescription = ObjectUtils.chooseNotNull(getTextFromTaskTextFile(taskTextFile), task.getDescriptionText());
    if (taskTextFile != null && TASK_MD.equals(taskTextFile.getName())) {
      return EduUtilsKt.convertToHtml(taskDescription);
    }
    return taskDescription;
  }

  @Nullable
  private static VirtualFile getTaskTextFile(@NotNull VirtualFile taskDirectory) {
    VirtualFile taskTextFile = taskDirectory.findChild(TASK_HTML);
    if (taskTextFile == null) {
      taskTextFile = taskDirectory.findChild(TASK_MD);
    }
    return taskTextFile;
  }

  @Nullable
  public static String getTextFromTaskTextFile(@Nullable VirtualFile taskTextFile) {
    if (taskTextFile != null) {
      Document document = FileDocumentManager.getInstance().getDocument(taskTextFile);
      return document == null ? null : document.getText();
    }
    return null;
  }

}
