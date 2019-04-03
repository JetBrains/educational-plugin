package com.jetbrains.edu.learning.courseFormat;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.xmlb.annotations.Transient;
import com.jetbrains.edu.learning.OpenApiExtKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class Section extends ItemContainer {
  public Section() {}

  @Transient
  public List<Integer> units = new ArrayList<>();  // should be used only during deserialization from stepik
  private int courseId;
  private int position;

  @Transient
  private Course myCourse;

  public void init(@Nullable Course course, @Nullable StudyItem parentItem, boolean isRestarted) {
    myCourse = course;
    int index = 1;

    if (course != null) {
      this.courseId = course.getId();
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
  @Nullable
  public VirtualFile getDir(@NotNull Project project) {
    return OpenApiExtKt.getCourseDir(project).findChild(getName());
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
}
