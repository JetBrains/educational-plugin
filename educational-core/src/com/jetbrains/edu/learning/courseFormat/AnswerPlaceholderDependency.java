package com.jetbrains.edu.learning.courseFormat;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.xmlb.annotations.Transient;
import com.jetbrains.edu.learning.EduNames;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.courseFormat.ext.TaskExt;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AnswerPlaceholderDependency {
  private static final Pattern DEPENDENCY_PATTERN = Pattern.compile("lesson(\\d+)#task(\\d+)#([^#]+)#(\\d+)");

  @Expose
  @SerializedName("lesson")
  private int myLessonIndex;

  @Expose
  @SerializedName("task")
  private int myTaskIndex;

  @Expose
  @SerializedName("file")
  private String myFile;

  @Expose
  @SerializedName("placeholder")
  private int myPlaceholderIndex;

  private AnswerPlaceholder myAnswerPlaceholder = null;

  public AnswerPlaceholderDependency() {
  }

  public AnswerPlaceholderDependency(@NotNull AnswerPlaceholder answerPlaceholder, int lessonIndex, int taskIndex, String fileName, int placeholderIndex) {
    myLessonIndex = lessonIndex;
    myTaskIndex = taskIndex;
    myFile = fileName;
    myPlaceholderIndex = placeholderIndex;
    myAnswerPlaceholder = answerPlaceholder;
  }

  @Nullable
  public AnswerPlaceholder resolve(@NotNull Course course) {
    if (!EduUtils.indexIsValid(myLessonIndex, course.getLessons())) {
      return null;
    }
    Lesson lesson = course.getLessons().get(myLessonIndex);
    if (!EduUtils.indexIsValid(myTaskIndex, lesson.getTaskList())) {
      return null;
    }
    Task task = lesson.getTaskList().get(myTaskIndex);
    TaskFile taskFile = task.getTaskFile(myFile);
    if (taskFile == null) {
      return null;
    }
    if (!EduUtils.indexIsValid(myPlaceholderIndex, taskFile.getAnswerPlaceholders())) {
      return null;
    }
    return taskFile.getAnswerPlaceholders().get(myPlaceholderIndex);
  }

  @Transient
  public AnswerPlaceholder getAnswerPlaceholder() {
    return myAnswerPlaceholder;
  }

  @Transient
  public void setAnswerPlaceholder(AnswerPlaceholder answerPlaceholder) {
    myAnswerPlaceholder = answerPlaceholder;
  }

  @Nullable
  public static AnswerPlaceholderDependency create(@NotNull AnswerPlaceholder answerPlaceholder, @NotNull String text)
    throws InvalidDependencyException {
    if (text.isEmpty()) {
      return null;
    }
    Task task = answerPlaceholder.getTaskFile().getTask();
    Course course = TaskExt.getCourse(task);
    if (course == null) {
      throw new InvalidDependencyException(text, "unable to retrieve course from source placeholder");
    }
    Matcher matcher = DEPENDENCY_PATTERN.matcher(text);
    if (!matcher.matches()) {
      throw new InvalidDependencyException(text);
    }
    try {
      int lessonIndex = Integer.parseInt(matcher.group(1)) - 1;
      int taskIndex = Integer.parseInt(matcher.group(2)) - 1;
      if (refersToNextTask(task, lessonIndex, taskIndex)) {
        throw new InvalidDependencyException(text, "dependencies should refer to previous tasks, not next ones");
      }
      String file = FileUtil.toSystemIndependentName(matcher.group(3));
      int placeholderIndex = Integer.parseInt(matcher.group(4)) - 1;
      AnswerPlaceholderDependency dependency = new AnswerPlaceholderDependency(answerPlaceholder, lessonIndex, taskIndex, file, placeholderIndex);
      AnswerPlaceholder targetPlaceholder = dependency.resolve(course);
      if (targetPlaceholder == null) {
        throw new InvalidDependencyException(text, "non existing answer placeholder");
      }
      if (targetPlaceholder.getTaskFile().getTask() == task) {
        throw new InvalidDependencyException(text, "dependencies should refer to tasks other than source one");
      }
      return dependency;
    }
    catch (NumberFormatException e) {
      throw new InvalidDependencyException(text);
    }
  }

  private static boolean refersToNextTask(Task task, int lessonIndex, int taskIndex) {
    int visibleLessonIndex = lessonIndex + 1;
    if (visibleLessonIndex != task.getLesson().getIndex()) {
      return visibleLessonIndex > task.getLesson().getIndex();
    }
    int visibleTaskIndex = taskIndex + 1;
    return visibleTaskIndex > task.getIndex();
  }

  public int getLessonIndex() {
    return myLessonIndex;
  }

  public void setLessonIndex(int lessonIndex) {
    myLessonIndex = lessonIndex;
  }

  public int getTaskIndex() {
    return myTaskIndex;
  }

  public void setTaskIndex(int taskIndex) {
    myTaskIndex = taskIndex;
  }

  public String getFile() {
    return myFile;
  }

  public void setFile(String file) {
    myFile = file;
  }

  public int getPlaceholderIndex() {
    return myPlaceholderIndex;
  }

  public void setPlaceholderIndex(int placeholderIndex) {
    myPlaceholderIndex = placeholderIndex;
  }

  @Override
  public String toString() {
    return EduNames.LESSON + (myLessonIndex + 1) + "#" + EduNames.TASK + (myTaskIndex + 1) + "#" + myFile + "#" + (myPlaceholderIndex + 1);
  }

  public static class InvalidDependencyException extends IllegalStateException {
    private final String myCustomMessage;

    public InvalidDependencyException(String dependencyText) {
      super("'" + dependencyText + "'" + " is not a valid placeholder dependency");
      myCustomMessage = "invalid dependency";
    }

    public InvalidDependencyException(String dependencyText, String customMessage) {
      super("'" + dependencyText + "'" + " is not a valid placeholder dependency\n" + customMessage);
      myCustomMessage = customMessage;
    }

    public String getCustomMessage() {
      return myCustomMessage;
    }
  }
}
