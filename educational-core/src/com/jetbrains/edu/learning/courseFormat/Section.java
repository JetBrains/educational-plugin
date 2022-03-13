package com.jetbrains.edu.learning.courseFormat;

import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class Section extends LessonContainer {
  public Section() {}

  transient public List<Integer> units = new ArrayList<>();  // should be used only during deserialization from stepik
  private int position;

  transient private Course myCourse;

  public void init(@Nullable Course course, @Nullable StudyItem parentItem, boolean isRestarted) {
    myCourse = course;
    int index = 1;

    for (StudyItem lesson : items) {
      if (lesson instanceof Lesson) {
        lesson.setIndex(index);
        index++;
        lesson.init(course, this, isRestarted);
      }
    }
  }

  public void setPosition(int position) {
    this.position = position;
  }

  public int getPosition() {
    return position;
  }

  @Override
  @Nullable
  public VirtualFile getDir(@NotNull final VirtualFile baseDir) {
    return baseDir.findChild(getName());
  }

  @NotNull
  public Course getCourse() {
    return myCourse;
  }

  public void setCourse(Course course) {
    myCourse = course;
  }

  @NotNull
  @Override
  public StudyItem getParent() {
    return myCourse;
  }

  @Override
  public String getItemType() {
    return "section";
  }
}
