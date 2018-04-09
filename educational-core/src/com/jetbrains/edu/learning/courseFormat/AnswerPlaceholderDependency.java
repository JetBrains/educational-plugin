package com.jetbrains.edu.learning.courseFormat;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.xmlb.annotations.Transient;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.courseFormat.ext.TaskExt;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AnswerPlaceholderDependency {
  private static final Pattern DEPENDENCY_PATTERN = Pattern.compile("(([^#]+)#)?([^#]+)#([^#]+)#([^#]+)#(\\d+)");

  @Expose
  @SerializedName("section")
  @Nullable
  private String mySectionName;

  @Expose
  @SerializedName("lesson")
  private String myLessonName;

  @Expose
  @SerializedName("task")
  private String myTaskName;

  @Expose
  @SerializedName("file")
  private String myFileName;

  @Expose
  @SerializedName("placeholder")
  private int myPlaceholderIndex;

  private AnswerPlaceholder myAnswerPlaceholder = null;

  public AnswerPlaceholderDependency() {
  }

  public AnswerPlaceholderDependency(@NotNull AnswerPlaceholder answerPlaceholder,
                                     @Nullable String sectionName,
                                     @NotNull String lessonName,
                                     @NotNull String taskName,
                                     @NotNull String fileName,
                                     int placeholderIndex) {
    mySectionName = sectionName;
    myLessonName = lessonName;
    myTaskName = taskName;
    myFileName = fileName;
    myPlaceholderIndex = placeholderIndex;
    myAnswerPlaceholder = answerPlaceholder;
  }

  @Nullable
  public AnswerPlaceholder resolve(@NotNull Course course) {
    Lesson lesson = course.getLesson(mySectionName, myLessonName);
    if (lesson == null) {
      return null;
    }
    Task task = lesson.getTask(myTaskName);
    if (task == null) {
      return null;
    }
    TaskFile taskFile = task.getTaskFile(myFileName);
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
    if (StringUtil.isEmptyOrSpaces(text)) {
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
      String sectionName = matcher.group(2);
      String lessonName = matcher.group(3);
      String taskName = matcher.group(4);
      String file = FileUtil.toSystemIndependentName(matcher.group(5));
      int placeholderIndex = Integer.parseInt(matcher.group(6)) - 1;
      AnswerPlaceholderDependency dependency = new AnswerPlaceholderDependency(answerPlaceholder, sectionName, lessonName, taskName, file, placeholderIndex);
      AnswerPlaceholder targetPlaceholder = dependency.resolve(course);
      if (targetPlaceholder == null) {
        throw new InvalidDependencyException(text, "non existing answer placeholder");
      }
      if (targetPlaceholder.getTaskFile().getTask() == task) {
        throw new InvalidDependencyException(text, "dependencies should refer to tasks other than source one");
      }
      if (refersToNextTask(task, targetPlaceholder.getTaskFile().getTask())) {
        throw new InvalidDependencyException(text, "dependencies should refer to previous tasks, not next ones");
      }
      return dependency;
    }
    catch (NumberFormatException e) {
      throw new InvalidDependencyException(text);
    }
  }

  private static boolean refersToNextTask(@NotNull Task sourceTask, @NotNull Task targetTask) {
    Lesson sourceLesson = sourceTask.getLesson();
    Lesson targetLesson = targetTask.getLesson();
    if (sourceLesson == targetLesson) {
      return targetTask.getIndex() > sourceTask.getIndex();
    }
    if (sourceLesson.getSection() == targetLesson.getSection()) {
      return targetLesson.getIndex() > sourceLesson.getIndex();
    }
    return getIndexInCourse(targetLesson) > getIndexInCourse(sourceLesson);

  }

  private static int getIndexInCourse(@NotNull Lesson lesson) {
    Section section = lesson.getSection();
    return section != null ? section.getIndex() : lesson.getIndex();
  }

  @Nullable
  public String getSectionName() {
    return mySectionName;
  }

  public void setSectionName(@Nullable String sectionName) {
    mySectionName = sectionName;
  }

  public String getLessonName() {
    return myLessonName;
  }

  public void setLessonName(String lessonName) {
    myLessonName = lessonName;
  }

  public String getTaskName() {
    return myTaskName;
  }

  public void setTaskName(String taskName) {
    myTaskName = taskName;
  }

  public String getFileName() {
    return myFileName;
  }

  public void setFileName(String fileName) {
    myFileName = fileName;
  }

  public int getPlaceholderIndex() {
    return myPlaceholderIndex;
  }

  public void setPlaceholderIndex(int placeholderIndex) {
    myPlaceholderIndex = placeholderIndex;
  }

  @Override
  public String toString() {
    String section = mySectionName != null ? mySectionName + "#" : "";
    return section + StringUtil.join(ContainerUtil.newArrayList(myLessonName, myTaskName, myFileName, myPlaceholderIndex + 1), "#");
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
