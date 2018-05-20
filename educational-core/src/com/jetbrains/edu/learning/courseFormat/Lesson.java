package com.jetbrains.edu.learning.courseFormat;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.xmlb.annotations.AbstractCollection;
import com.intellij.util.xmlb.annotations.Transient;
import com.jetbrains.edu.learning.EduNames;
import com.jetbrains.edu.learning.EduUtils;
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
    OutputTask.class,
    IdeTask.class,
    VideoTask.class
  })
  public List<Task> taskList = new ArrayList<>();

  @Transient
  private Course myCourse = null;

  @Transient
  private Section mySection = null;

  public Lesson() {
  }

  public void init(@Nullable final Course course, @Nullable final StudyItem section, boolean isRestarted) {
    mySection = section instanceof Section ? (Section)section : null;
    setCourse(course);
    List<Task> tasks = getTaskList();
    for (int i = 0; i < tasks.size(); i++) {
      Task task = tasks.get(i);
      task.setIndex(i + 1);
      task.init(course, this, isRestarted);
    }
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
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

  @Nullable
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
    Lesson lessonInfo = StepikConnector.getLessonFromServer(myId);
    if (lessonInfo == null) return true;
    if (lessonInfo.myUpdateDate == null) return true;
    if (myUpdateDate == null) return false;
    if (lessonInfo.steps.size() != taskList.size()) return false;
    for (Task task : taskList) {
      if (!task.isUpToDate()) return false;
    }
    return !lessonInfo.myUpdateDate.after(myUpdateDate);
  }

  public boolean isAdditional() {
    // We still use `StepikNames.PYCHARM_ADDITIONAL` while Stepik interaction
    // so we need to check it here
    return EduNames.ADDITIONAL_MATERIALS.equals(name) || StepikNames.PYCHARM_ADDITIONAL.equals(name);
  }

  @Transient
  @Nullable
  public Section getSection() {
    return mySection;
  }

  @Transient
  public void setSection(@Nullable Section section) {
    mySection = section;
  }

  @Nullable
  public VirtualFile getLessonDir(@NotNull final Project project) {
    VirtualFile courseDir = EduUtils.getCourseDir(project);

    if (mySection == null) {
      return courseDir.findChild(getName());
    }
    else {
      VirtualFile sectionDir = courseDir.findChild(mySection.getName());
      assert sectionDir != null : "Section dir for lesson not found";

      return sectionDir.findChild(getName());
    }
  }

  @NotNull
  public ItemContainer getContainer() {
    if (mySection != null) {
      return mySection;
    }
    return myCourse;
  }
}
