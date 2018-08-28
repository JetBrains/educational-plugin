package com.jetbrains.edu.learning.courseFormat;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.xmlb.annotations.Transient;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.courseFormat.ext.StepikCourseExt;
import com.jetbrains.edu.learning.courseFormat.remote.RemoteInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Date;
import java.util.List;

public class Section extends ItemContainer {
  @NotNull RemoteInfo myRemoteInfo = new RemoteInfo() {};

  public List<Integer> units;
  @SerializedName("course")
  private int courseId;
  @Expose
  @SerializedName("title")
  private String name;

  private int position;
  @Expose
  @SerializedName("update_date")
  private Date myUpdateDate = new Date(0);

  @Transient
  private Course myCourse;

  public void init(@Nullable Course course, @Nullable StudyItem parentItem, boolean isRestarted) {
    myCourse = course;
    int index = 1;

    if (course != null) {
      this.courseId = StepikCourseExt.getId(course);
    }
    for (StudyItem lesson : items) {
      if (lesson instanceof Lesson) {
        lesson.setIndex(index);
        index++;
        lesson.init(course, this, isRestarted);
      }
    }
  }

  public void setCourseId(int courseId) {
    this.courseId = courseId;
  }

  public void setPosition(int position) {
    this.position = position;
  }

  public int getCourseId() {
    return courseId;
  }

  public int getPosition() {
    return position;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public void setName(String name) {
    this.name = name;
  }

  public void setUpdateDate(Date updateDate) {
    myUpdateDate = updateDate;
  }

  public Date getUpdateDate() {
    return myUpdateDate;
  }

  @Override
  @Nullable
  public VirtualFile getDir(@NotNull Project project) {
    return EduUtils.getCourseDir(project).findChild(getName());
  }

  @Transient
  @NotNull
  public Course getCourse() {
    return myCourse;
  }

  @Transient
  public void setCourse(Course course) {
    myCourse = course;
  }

  @NotNull
  @Override
  public StudyItem getParent() {
    return myCourse;
  }

  @NotNull
  public RemoteInfo getRemoteInfo() {
    return myRemoteInfo;
  }

  public void setRemoteInfo(@NotNull RemoteInfo remoteInfo) {
    myRemoteInfo = remoteInfo;
  }
}
