package com.jetbrains.edu.learning.courseFormat;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.intellij.util.xmlb.annotations.AbstractCollection;
import com.intellij.util.xmlb.annotations.Transient;
import com.jetbrains.edu.learning.EduNames;
import com.jetbrains.edu.learning.courseFormat.tasks.*;
import com.jetbrains.edu.learning.stepik.StepikConnector;
import com.jetbrains.edu.learning.stepik.StepikNames;
import kotlin.collections.CollectionsKt;
import one.util.streamex.StreamEx;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Lesson extends StudyItem {
  @Expose @SerializedName("id") private int myId;
  @Transient public List<Integer> steps;
  @Transient public List<String> tags;
  @Transient boolean is_public;
  @Expose @SerializedName("update_date") private Date myUpdateDate;

  @Expose
  @SerializedName("title")
  private String name;

  @Expose
  @SerializedName("task_list")
  @AbstractCollection(elementTypes = {
    EduTask.class,
    ChoiceTask.class,
    TheoryTask.class,
    CodeTask.class,
    TaskWithSubtasks.class,
    OutputTask.class,
    IdeTask.class
  })
  public List<Task> taskList = new ArrayList<>();

  @Transient
  private Course myCourse = null;

  @Expose
  @SerializedName("is_framework_lesson")
  private boolean isFrameworkLesson;

  public Lesson() {
  }

  public void initLesson(final Course course, boolean isRestarted) {
    setCourse(course);
    for (Task task : getTaskList()) {
      task.initTask(this, isRestarted);
    }
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public List<Task> getTaskList() {
    return taskList;
  }

  public List<Task> getTaskListForProgress() {
    return CollectionsKt.filter(taskList, task -> !(task instanceof TheoryTask));
  }

  @Transient
  public Course getCourse() {
    return myCourse;
  }

  @Transient
  public void setCourse(Course course) {
    myCourse = course;
  }

  public void addTask(@NotNull final Task task) {
    taskList.add(task);
  }

  public Task getTask(@NotNull final String name) {
    return StreamEx.of(taskList).findFirst(task -> name.equals(task.getName())).orElse(null);
  }

  public Task getTask(int id) {
    for (Task task : taskList) {
      if (task.getStepId() == id) {
        return task;
      }
    }
    return null;
  }

  public void updateTaskList(List<Task> taskList) {
    this.taskList = taskList;
  }

  public CheckStatus getStatus() {
    for (Task task : taskList) {
      if (task.getStatus() != CheckStatus.Solved) {
        return CheckStatus.Unchecked;
      }
    }
    return CheckStatus.Solved;
  }

  public int getId() {
    return myId;
  }

  public void setId(int id) {
    this.myId = id;
  }

  public Date getUpdateDate() {
    return myUpdateDate;
  }

  public void setUpdateDate(Date updateDate) {
    myUpdateDate = updateDate;
  }

  public void setPublic(boolean isPublic) {
    this.is_public = isPublic;
  }

  public boolean isUpToDate() {
    if (myId == 0) return true;
    final Date date = StepikConnector.getLessonUpdateDate(myId);
    if (date == null) return true;
    if (myUpdateDate == null) return false;
    for (Task task : taskList) {
      if (!task.isUpToDate()) return false;
    }
    return !date.after(myUpdateDate);
  }

  public boolean isAdditional() {
    // We still use `StepikNames.PYCHARM_ADDITIONAL` while Stepik interaction
    // so we need to check it here
    return EduNames.ADDITIONAL_MATERIALS.equals(name) || StepikNames.PYCHARM_ADDITIONAL.equals(name);
  }

  public boolean isFrameworkLesson() {
    return isFrameworkLesson;
  }

  public void setFrameworkLesson(boolean frameworkLesson) {
    isFrameworkLesson = frameworkLesson;
  }
}
