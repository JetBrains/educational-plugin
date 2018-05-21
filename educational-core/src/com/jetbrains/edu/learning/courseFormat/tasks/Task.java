package com.jetbrains.edu.learning.courseFormat.tasks;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.xmlb.XmlSerializer;
import com.intellij.util.xmlb.annotations.Transient;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.checker.TaskCheckerProvider;
import com.jetbrains.edu.learning.courseFormat.*;
import com.jetbrains.edu.learning.courseFormat.ext.TaskExt;
import com.jetbrains.edu.learning.serialization.SerializationUtils;
import com.jetbrains.edu.learning.stepik.StepikConnector;
import com.jetbrains.edu.learning.stepik.StepikTaskBuilder;
import com.jetbrains.edu.learning.stepik.StepikUtils;
import icons.EducationalCoreIcons;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of task which contains task files, tests, input file for tests
 *
 * To implement new task there are 5 steps to be done:
 * - Extend {@link Task} class
 * - Go to {@link Lesson#taskList} and update elementTypes in AbstractCollection annotation. Needed for proper xml serialization
 * - Update {@link SerializationUtils.Json.TaskAdapter#deserialize} to handle json serialization
 * - Update {@link TaskCheckerProvider#getTaskChecker} and provide default checker for new task
 * - Update {@link StepikTaskBuilder#pluginTaskTypes} for the tasks we do not have separately on stepik and {@link StepikTaskBuilder#stepikTaskTypes} otherwise
 */
public abstract class Task extends StudyItem {
  @Expose private String name;

  protected CheckStatus myStatus = CheckStatus.Unchecked;

  @SerializedName("stepic_id")
  @Expose private int myStepId;

  @SerializedName("task_files")
  @Expose public Map<String, TaskFile> taskFiles = new HashMap<>();

  @SerializedName("test_files")
  @Expose protected Map<String, String> testsText = new HashMap<>();

  @SerializedName("description_text")
  @Expose private String descriptionText;

  @SerializedName("description_format")
  @Expose private DescriptionFormat descriptionFormat = EduUtils.getDefaultTaskDescriptionFormat();

  @SerializedName("additional_files")
  @Expose protected Map<String, String> additionalFiles = new HashMap<>();

  @Transient private Lesson myLesson;
  @Expose @SerializedName("update_date") private Date myUpdateDate;

  public Task() {}

  public Task(@NotNull final String name) {
    this.name = name;
  }

  public void init(@Nullable Course course, @Nullable final StudyItem parentItem, boolean isRestarted) {
    setLesson(parentItem instanceof Lesson ? (Lesson)parentItem : null);
    if (!isRestarted) myStatus = CheckStatus.Unchecked;
    for (TaskFile taskFile : getTaskFiles().values()) {
      taskFile.initTaskFile(this, isRestarted);
    }
  }

  public Map<String, TaskFile> getTaskFiles() {
    return taskFiles;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public void setName(String name) {
    this.name = name;
  }

  public String getDescriptionText() {
    return descriptionText;
  }

  public void setDescriptionText(String descriptionText) {
    this.descriptionText = descriptionText;
  }

  public DescriptionFormat getDescriptionFormat() {
    return descriptionFormat;
  }

  public void setDescriptionFormat(DescriptionFormat descriptionFormat) {
    this.descriptionFormat = descriptionFormat;
  }

  public Map<String, String> getTestsText() {
    return testsText;
  }

  @SuppressWarnings("unused")
  //used for deserialization
  public void setTestsText(Map<String, String> testsText) {
    this.testsText = testsText;
  }

  public Map<String, String> getAdditionalFiles() {
    return additionalFiles;
  }

  @SuppressWarnings("unused")
  //used for deserialization
  public void setAdditionalFiles(Map<String, String> additionalFiles) {
    this.additionalFiles = additionalFiles;
  }

  public void addTestsTexts(String name, String text) {
    testsText.put(name, text);
  }

  public void addAdditionalFile(String name, String text) {
    additionalFiles.put(name, text);
  }

  @Nullable
  public TaskFile getTaskFile(final String name) {
    return name != null ? taskFiles.get(name) : null;
  }

  public TaskFile addTaskFile(@NotNull final String name) {
    TaskFile taskFile = new TaskFile();
    taskFile.setTask(this);
    taskFile.name = name;
    taskFiles.put(name, taskFile);
    return taskFile;
  }

  public void addTaskFile(@NotNull final TaskFile taskFile) {
    taskFiles.put(taskFile.name, taskFile);
  }

  @Nullable
  public TaskFile getFile(@NotNull final String fileName) {
    return taskFiles.get(fileName);
  }

  @Transient
  public Lesson getLesson() {
    return myLesson;
  }

  @Transient
  public void setLesson(Lesson lesson) {
    myLesson = lesson;
  }

  @Nullable
  public VirtualFile getTaskDir(@NotNull final Project project) {
    final VirtualFile lessonDir = myLesson.getLessonDir(project);

    return lessonDir == null ? null : lessonDir.findChild(TaskExt.getDirName(this));
  }

  /**
   * @param wrap if true, text will be wrapped with ancillary information (e.g. to display latex)
   */
  public String getTaskDescription(boolean wrap, @Nullable VirtualFile taskDir) {
    String taskText = descriptionText;
    if (!wrap) {
      return taskText;
    }
    if (taskDir != null) {
      StringBuffer text = new StringBuffer(taskText);
      EduUtils.replaceActionIDsWithShortcuts(text);
      taskText = text.toString();
      if (descriptionFormat == DescriptionFormat.MD) {
        taskText = EduUtils.convertToHtml(taskText, taskDir);
      }
    }
    if (getLesson().getCourse() instanceof RemoteCourse && taskText != null) {
      taskText = StepikUtils.wrapStepikTasks(this, taskText, getLesson().getCourse().isAdaptive());
    }
    return taskText;
  }

  @Nullable
  public String getTaskDescription(@Nullable VirtualFile taskDir) {
    if (taskDir == null) {
      return null;
    }
    return getTaskDescription(true, taskDir);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Task task = (Task)o;

    if (getIndex() != task.getIndex()) return false;
    if (name != null ? !name.equals(task.name) : task.name != null) return false;
    if (taskFiles != null ? !taskFiles.equals(task.taskFiles) : task.taskFiles != null) return false;
    if (descriptionText != null ? !descriptionText.equals(task.descriptionText) : task.descriptionText != null) return false;
    if (descriptionFormat != null ? !descriptionFormat.equals(task.descriptionFormat) : task.descriptionFormat != null) return false;
    if (testsText != null ? !testsText.equals(task.testsText) : task.testsText != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = name != null ? name.hashCode() : 0;
    result = 31 * result + getIndex();
    result = 31 * result + (taskFiles != null ? taskFiles.hashCode() : 0);
    result = 31 * result + (descriptionText != null ? descriptionText.hashCode() : 0);
    result = 31 * result + (descriptionFormat != null ? descriptionFormat.hashCode() : 0);
    result = 31 * result + (testsText != null ? testsText.hashCode() : 0);
    return result;
  }

  public void setStepId(int stepId) {
    myStepId = stepId;
  }

  public int getStepId() {
    return myStepId;
  }

  public CheckStatus getStatus() {
    return myStatus;
  }

  public void setStatus(CheckStatus status) {
    for (TaskFile taskFile : taskFiles.values()) {
      for (AnswerPlaceholder placeholder : taskFile.getAnswerPlaceholders()) {
        placeholder.setStatus(status);
      }
    }
    myStatus = status;
  }

  public Task copy() {
    Element element = XmlSerializer.serialize(this);
    Task copy = XmlSerializer.deserialize(element, getClass());
    copy.init(null, null, true);
    return copy;
  }

  public void setUpdateDate(Date date) {
    myUpdateDate = date;
  }

  public Date getUpdateDate() {
    return myUpdateDate;
  }

  public boolean isUpToDate() {
    if (getStepId() == 0) return true;
    final Date date = StepikConnector.getTaskUpdateDate(getStepId());
    if (date == null) return true;
    if (myUpdateDate == null) return false;
    return !date.after(myUpdateDate);
  }

  // used in json serialization/deserialization
  public abstract String getTaskType();

  public int getPosition() {
    final Lesson lesson = getLesson();
    return lesson.getTaskList().indexOf(this) + 1;
  }

  public boolean isValid(@NotNull Project project) {
    VirtualFile taskDir = getTaskDir(project);
    if (taskDir == null) return false;
    for (TaskFile taskFile : getTaskFiles().values()) {
      VirtualFile file = EduUtils.findTaskFileInDir(taskFile, taskDir);
      if (file == null) continue;
      try {
        String text = VfsUtilCore.loadText(file);
        if (!taskFile.isValid(text)) return false;
      }
      catch (IOException e) {
        return false;
      }
    }
    return true;
  }

  public boolean isToSubmitToStepik() {
    return false;
  }

  public Icon getIcon() {
    if (myStatus == CheckStatus.Unchecked) {
      return EducationalCoreIcons.Task;
    }
    return myStatus == CheckStatus.Solved ? EducationalCoreIcons.TaskSolved : EducationalCoreIcons.TaskFailed;
  }
}
