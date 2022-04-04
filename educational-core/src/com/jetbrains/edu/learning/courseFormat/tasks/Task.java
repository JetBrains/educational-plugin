package com.jetbrains.edu.learning.courseFormat.tasks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.edu.EducationalCoreIcons;
import com.jetbrains.edu.coursecreator.StudyItemTypeKt;
import com.jetbrains.edu.coursecreator.stepik.StepikChangeRetriever;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.OpenApiExtKt;
import com.jetbrains.edu.learning.actions.CheckAction;
import com.jetbrains.edu.learning.checker.TaskCheckerProvider;
import com.jetbrains.edu.learning.courseFormat.*;
import com.jetbrains.edu.learning.courseFormat.ext.CourseExt;
import com.jetbrains.edu.learning.courseFormat.ext.TaskExt;
import com.jetbrains.edu.learning.messages.EduCoreBundle;
import com.jetbrains.edu.learning.stepik.StepikTaskBuilder;
import com.jetbrains.edu.learning.stepik.api.StepikJacksonDeserializersKt;
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse;
import com.jetbrains.edu.learning.submissions.SubmissionsManager;
import com.jetbrains.edu.learning.yaml.YamlDeserializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.IOException;
import java.util.*;

import static com.jetbrains.edu.coursecreator.StudyItemType.TASK_TYPE;

/**
 * Implementation of task which contains task files, tests, input file for tests
 * Update {@link StepikChangeRetriever#taskFilesChanged and StepikChangeRetriever#taskInfoChanged} if you added new property that has to be compared
 * To implement new task there are 5 steps to be done:
 * - Extend {@link Task} class
 * - Update {@link StepikJacksonDeserializersKt#doDeserializeTask} to handle json serialization
 * - Update {@link TaskCheckerProvider#getTaskChecker} and provide default checker for new task
 * - Update {@link StepikTaskBuilder#pluginTaskTypes} for the tasks we do not have separately on stepik and {@link StepikTaskBuilder.StepikTaskType} otherwise
 * - Handle yaml deserialization:
 *    - add type in {@link YamlDeserializer#deserializeTask(ObjectMapper, String)}
 *    - add yaml mixins for course creator and student fields {@link com.jetbrains.edu.learning.yaml.format}
 */
public abstract class Task extends StudyItem {
  protected static final Logger LOG = Logger.getInstance(Task.class);
  @NotNull
  protected CheckStatus myStatus = CheckStatus.Unchecked;
  @Nullable
  private CheckFeedback myFeedback;
  private Map<String, TaskFile> myTaskFiles = new LinkedHashMap<>();
  @NotNull
  private String descriptionText = "";
  private DescriptionFormat descriptionFormat = EduUtils.getDefaultTaskDescriptionFormat();
  @Nullable
  private String myFeedbackLink = null;
  @Nullable
  private Boolean solutionHidden;
  private int myRecord = -1;
  private boolean isUpToDate = true;
  // Used for marketplace courses. We need to store a meta-entity id (corresponding to list of submissions) to correctly process submissions
  // storage on grazie platform
  @Nullable
  private String submissionsId;

  //used for deserialization
  public Task() { }

  public Task(@NotNull final String name) {
    super(name);
  }

  public Task(@NotNull final String name, int id, int position, @NotNull Date updateDate, @NotNull CheckStatus status) {
    super(name);
    setId(id);
    setIndex(position);
    setUpdateDate(updateDate);
    myStatus = status;
  }

  public void init(@NotNull final ItemContainer parentItem, boolean isRestarted) {
    if (!(parentItem instanceof Lesson)) throw new IllegalStateException("Parent for task should be lesson");
    setParent((Lesson)parentItem);
    for (TaskFile taskFile : getTaskFileValues()) {
      taskFile.initTaskFile(this, isRestarted);
    }
  }

  public Map<String, TaskFile> getTaskFiles() {
    return myTaskFiles;
  }

  // Use carefully. taskFiles is supposed to be ordered so use LinkedHashMap
  public void setTaskFiles(Map<String, TaskFile> taskFiles) {
    this.myTaskFiles = taskFiles;
  }

  @NotNull
  public String getDescriptionText() {
    return descriptionText;
  }

  public void setDescriptionText(@NotNull String descriptionText) {
    this.descriptionText = descriptionText;
  }

  public DescriptionFormat getDescriptionFormat() {
    return descriptionFormat;
  }

  public void setDescriptionFormat(DescriptionFormat descriptionFormat) {
    this.descriptionFormat = descriptionFormat;
  }

  public boolean isUpToDate() {
    return isUpToDate;
  }

  public void setUpToDate(boolean upToDate) {
    isUpToDate = upToDate;
  }

  @Nullable
  public TaskFile getTaskFile(@Nullable final String name) {
    return name != null ? myTaskFiles.get(name) : null;
  }

  public TaskFile addTaskFile(@NotNull final String name, boolean isVisible) {
    TaskFile taskFile = new TaskFile();
    taskFile.setTask(this);
    taskFile.setName(name);
    taskFile.setVisible(isVisible);
    myTaskFiles.put(name, taskFile);
    return taskFile;
  }

  public TaskFile addTaskFile(@NotNull final String name) {
    return addTaskFile(name, true);
  }

  public void addTaskFile(@NotNull final TaskFile taskFile) {
    taskFile.setTask(this);
    myTaskFiles.put(taskFile.getName(), taskFile);
  }

  public void addTaskFile(@NotNull final TaskFile taskFile, int position) {
    taskFile.setTask(this);
    if (position < 0 || position > myTaskFiles.size()) {
      throw new IndexOutOfBoundsException();
    }
    Map<String, TaskFile> newTaskFileMap = new LinkedHashMap<>(myTaskFiles.size() + 1);
    int currentIndex = 0;
    for (Map.Entry<String, TaskFile> entry : myTaskFiles.entrySet()) {
      if (currentIndex == position) {
        newTaskFileMap.put(taskFile.getName(), taskFile);
      }
      newTaskFileMap.put(entry.getKey(), entry.getValue());
      currentIndex++;
    }
    if (currentIndex == position) {
      newTaskFileMap.put(taskFile.getName(), taskFile);
    }
    myTaskFiles = newTaskFileMap;
  }

  @Nullable
  public TaskFile getFile(@NotNull final String fileName) {
    return myTaskFiles.get(fileName);
  }

  public Lesson getLesson() {
    ItemContainer parent = getParent();
    if (parent instanceof Lesson) {
      return (Lesson)parent;
    }
    else {
      return null;
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Task task = (Task)o;

    if (getIndex() != task.getIndex()) return false;
    if (!Objects.equals(getName(), task.getName())) return false;
    if (!Objects.equals(myTaskFiles, task.myTaskFiles)) return false;
    if (!descriptionText.equals(task.descriptionText)) return false;
    if (!Objects.equals(descriptionFormat, task.descriptionFormat)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = getName().hashCode();
    result = 31 * result + getIndex();
    result = 31 * result + (myTaskFiles != null ? myTaskFiles.hashCode() : 0);
    result = 31 * result + descriptionText.hashCode();
    result = 31 * result + (descriptionFormat != null ? descriptionFormat.hashCode() : 0);
    return result;
  }

  public @NotNull CheckStatus getStatus() {
    return myStatus;
  }

  public void setStatus(@NotNull CheckStatus status) {
    for (TaskFile taskFile : myTaskFiles.values()) {
      for (AnswerPlaceholder placeholder : taskFile.getAnswerPlaceholders()) {
        placeholder.setStatus(status);
      }
    }
    if (myStatus != status) {
      myFeedback = null;
    }
    myStatus = status;
  }

  @NotNull
  public Task copy() {
    return CopyUtilKt.copyAs(this, getClass());
  }

  public int getPosition() {
    final Lesson lesson = getLesson();
    return lesson.getTaskList().indexOf(this) + 1;
  }

  public boolean isValid(@NotNull Project project) {
    VirtualFile taskDir = getDir(OpenApiExtKt.getCourseDir(project));
    if (taskDir == null) return false;
    for (TaskFile taskFile : getTaskFileValues()) {
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

  public boolean isToSubmitToRemote() {
    return false;
  }

  public Icon getIcon() {
    if (myStatus == CheckStatus.Unchecked) {
      return EducationalCoreIcons.Task;
    }
    Project project = CourseExt.getProject(getCourse());
    if (project != null && SubmissionsManager.getInstance(project).containsCorrectSubmission(getId())) {
      return EducationalCoreIcons.TaskSolved;
    }
    return myStatus == CheckStatus.Solved ? EducationalCoreIcons.TaskSolved : EducationalCoreIcons.TaskFailed;
  }

  @Nullable
  public String getFeedbackLink() {
    return myFeedbackLink;
  }

  public void setFeedbackLink(@Nullable String feedbackLink) {
    myFeedbackLink = feedbackLink;
  }

  @Nullable
  @Override
  public VirtualFile getDir(@NotNull final VirtualFile courseDir) {
    final VirtualFile lessonDir = getLesson().getDir(courseDir);
    return TaskExt.findDir(this, lessonDir);
  }

  @NotNull
  @Override
  public Course getCourse() {
    return getLesson().getCourse();
  }

  @NotNull
  private Collection<TaskFile> getTaskFileValues() {
    return getTaskFiles().values();
  }

  @SuppressWarnings("unused") //used for yaml deserialization
  private void setTaskFileValues(List<TaskFile> taskFiles) {
    this.myTaskFiles.clear();
    for (TaskFile taskFile : taskFiles) {
      this.myTaskFiles.put(taskFile.getName(), taskFile);
    }
  }

  public int getRecord() {
    return myRecord;
  }

  public void setRecord(int record) {
    myRecord = record;
  }

  @Nullable
  public CheckFeedback getFeedback() {
    return myFeedback;
  }

  public void setFeedback(@Nullable CheckFeedback feedback) {
    myFeedback = feedback;
  }

  @NotNull
  public String getUIName() {
    if (getCourse() instanceof HyperskillCourse) {
      if (this instanceof CodeTask) return EduCoreBundle.message("item.task.challenge");
      return EduCoreBundle.message("item.task.stage");
    }
    return StudyItemTypeKt.getPresentableName(TASK_TYPE);
  }

  public boolean supportSubmissions() {
    return false;
  }

  /**
   * @return null means that behaviour for this particular Task hasn't been configured by a user
   * and {@link Course#getSolutionsHidden()} should be used instead
   */
  @Nullable
  public Boolean getSolutionHidden() {
    return solutionHidden;
  }

  public void setSolutionHidden(@Nullable Boolean solutionHidden) {
    this.solutionHidden = solutionHidden;
  }

  public boolean isPluginTaskType() {
    return true;
  }

  @NotNull
  public CheckAction getCheckAction() {
    return getCourse().getCheckAction();
  }

  @Nullable
  public String getSubmissionsId() {
    return submissionsId;
  }

  public void setSubmissionsId(@Nullable String submissionsId) {
    this.submissionsId = submissionsId;
  }

  /**
   * If task description changes after failing, then method returns true. For retry button
   */
  public boolean isChangedOnFailed() {
    return false;
  }
}
