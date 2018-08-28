package com.jetbrains.edu.learning.courseFormat;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.xmlb.annotations.Transient;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.courseFormat.remote.LocalInfo;
import com.jetbrains.edu.learning.courseFormat.remote.RemoteInfo;
import com.jetbrains.edu.learning.stepik.courseFormat.StepikSectionRemoteInfo;
import com.jetbrains.edu.learning.stepik.courseFormat.ext.StepikStudyItemExt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Section extends ItemContainer {
  @NotNull private RemoteInfo myRemoteInfo = new LocalInfo();

  @Expose
  @SerializedName("title")
  private String name;

  @Transient
  private Course myCourse;

  public void init(@Nullable Course course, @Nullable StudyItem parentItem, boolean isRestarted) {
    myCourse = course;
    int index = 1;

    if (course != null) {
      final RemoteInfo remoteInfo = getRemoteInfo();
      if (remoteInfo instanceof StepikSectionRemoteInfo) {
        ((StepikSectionRemoteInfo)remoteInfo).setCourseId(StepikStudyItemExt.getId(course));
      }
    }
    for (StudyItem lesson : items) {
      if (lesson instanceof Lesson) {
        lesson.setIndex(index);
        index++;
        lesson.init(course, this, isRestarted);
      }
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
