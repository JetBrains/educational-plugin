package com.jetbrains.edu.learning.courseFormat.tasks;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.xmlb.XmlSerializer;
import com.intellij.util.xmlb.annotations.Transient;
import com.jetbrains.edu.learning.EduNames;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.checker.TaskCheckerProvider;
import com.jetbrains.edu.learning.courseFormat.*;
import com.jetbrains.edu.learning.serialization.SerializationUtils;
import com.jetbrains.edu.learning.stepic.StepikTaskBuilder;
import com.jetbrains.edu.learning.stepik.StepikConnector;
import com.jetbrains.edu.learning.stepik.StepikUtils;
import one.util.streamex.EntryStream;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
 * - For Adaptive tasks update {@link StepikTaskBuilder#taskTypes} so new task type can be added to a course
 */
public abstract class Task implements StudyItem {
  @Expose private String name;

  // index is visible to user number of task from 1 to task number
  private int myIndex;
  protected CheckStatus myStatus = CheckStatus.Unchecked;

  @SerializedName("stepic_id")
  @Expose private int myStepId;

  @SerializedName("task_files")
  @Expose public Map<String, TaskFile> taskFiles = new HashMap<>();

  @SerializedName("test_files")
  @Expose protected Map<String, String> testsText = new HashMap<>();
  @SerializedName("task_texts")
  @Expose protected Map<String, String> taskTexts = new HashMap<>();

  @SerializedName("additional_files")
  @Expose protected Map<String, String> additionalFiles = new HashMap<>();

  @Transient private Lesson myLesson;
  @Expose @SerializedName("update_date") private Date myUpdateDate;

  public Task() {}

  public Task(@NotNull final String name) {
    this.name = name;
  }

  /**
   * Initializes state of task file
   *
   * @param lesson lesson which task belongs to
   */
  public void initTask(final Lesson lesson, boolean isRestarted) {
    setLesson(lesson);
    if (!isRestarted) myStatus = CheckStatus.Unchecked;
    for (TaskFile taskFile : getTaskFiles().values()) {
      taskFile.initTaskFile(this, isRestarted);
    }
  }

  public Map<String, TaskFile> getTaskFiles() {
    return taskFiles;
  }

  @SuppressWarnings("unused")
  //used for deserialization
  public void setTaskTexts(Map<String, String> taskTexts) {
    this.taskTexts = taskTexts;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public void setName(String name) {
    this.name = name;
  }

  @Override
  public int getIndex() {
    return myIndex;
  }

  @Override
  public void setIndex(int index) {
    myIndex = index;
  }

  public Map<String, String> getTestsText() {
    return testsText;
  }

  @SuppressWarnings("unused")
  //used for deserialization
  public void setTestsText(Map<String, String> testsText) {
    this.testsText = testsText;
  }

  public Map<String, String> getTaskTexts() {
    return taskTexts;
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

  public void addTaskText(String name, String text) {
    taskTexts.put(name, text);
  }

  public void addAdditionalFile(String name, String text) {
    additionalFiles.put(name, text);
  }

  @Nullable
  public TaskFile getTaskFile(final String name) {
    return name != null ? taskFiles.get(name) : null;
  }

  public void addTaskFile(@NotNull final String name, int index) {
    TaskFile taskFile = new TaskFile();
    taskFile.setIndex(index);
    taskFile.setTask(this);
    taskFile.name = name;
    taskFiles.put(name, taskFile);
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
    String lessonDirName = EduNames.LESSON + String.valueOf(myLesson.getIndex());
    String taskDirName = EduNames.TASK + String.valueOf(myIndex);
    VirtualFile courseDir = project.getBaseDir();
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      final Module module = ModuleManager.getInstance(project).getModules()[0];
      courseDir = ModuleRootManager.getInstance(module).getContentRoots()[0];
    }
    if (courseDir != null) {
      VirtualFile lessonDir = courseDir.findChild(lessonDirName);
      if (lessonDir != null) {
        return lessonDir.findChild(taskDirName);
      }
    }
    return null;
  }

  /**
   * @param wrap if true, text will be wrapped with ancillary information (e.g. to display latex)
   */
  public String getTaskDescription(boolean wrap) {
    String fileName = getTaskDescriptionName();
    //TODO: replace this with simple get after implementing migration for taskTexts
    Map.Entry<String, String> entry =
      EntryStream.of(taskTexts).findFirst(e -> FileUtil.getNameWithoutExtension(e.getKey()).equals(fileName)).orElse(null);
    if (entry == null) {
      return null;
    }
    String taskText = entry.getValue();
    if (!wrap) {
      return taskText;
    }
    taskText = EduUtils.convertToHtml(taskText);
    if (getLesson().getCourse() instanceof RemoteCourse && taskText != null) {
      taskText = StepikUtils.wrapStepikTasks(this, taskText, getLesson().getCourse().isAdaptive());
    }
    return EduUtils.convertToHtml(taskText);
  }

  public String getTaskDescription() {
    return getTaskDescription(true);
  }

  protected String getTaskDescriptionName() {
    return FileUtil.getNameWithoutExtension(EduNames.TASK_HTML);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Task task = (Task)o;

    if (myIndex != task.myIndex) return false;
    if (name != null ? !name.equals(task.name) : task.name != null) return false;
    if (taskFiles != null ? !taskFiles.equals(task.taskFiles) : task.taskFiles != null) return false;
    if (taskTexts != null ? !taskTexts.equals(task.taskTexts) : task.taskTexts != null) return false;
    if (testsText != null ? !testsText.equals(task.testsText) : task.testsText != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = name != null ? name.hashCode() : 0;
    result = 31 * result + myIndex;
    result = 31 * result + (taskFiles != null ? taskFiles.hashCode() : 0);
    result = 31 * result + (taskTexts != null ? taskTexts.hashCode() : 0);
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
      for (AnswerPlaceholder placeholder : taskFile.getActivePlaceholders()) {
        placeholder.setStatus(status);
      }
    }
    myStatus = status;
  }

  public Task copy() {
    Element element = XmlSerializer.serialize(this);
    Task copy = XmlSerializer.deserialize(element, getClass());
    copy.initTask(null, true);
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

  public void copyTaskParameters(Task task) {
    setName(task.getName());
    setIndex(task.getIndex());
    setStatus(task.getStatus());
    setStepId(task.getStepId());
    taskFiles = task.getTaskFiles();
    testsText = task.getTestsText();
    taskTexts = task.getTaskTexts();
    setLesson(task.getLesson());
    setUpdateDate(task.getUpdateDate());
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
      if (file == null) return false;
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
}
