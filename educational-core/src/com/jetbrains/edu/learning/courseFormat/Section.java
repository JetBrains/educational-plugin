package com.jetbrains.edu.learning.courseFormat;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.jetbrains.edu.learning.stepik.StepikConnector;
import org.jetbrains.annotations.Nullable;

import java.util.Date;
import java.util.List;

public class Section extends ItemContainer {
  public List<Integer> units;
  private int course;
  @Expose
  @SerializedName("title")
  private String name;

  private int position;
  @Expose
  private int id;
  @Expose
  @SerializedName("update_date")
  private Date myUpdateDate;

  public void init(@Nullable Course course, @Nullable StudyItem parentItem, boolean isRestarted) {
    int index = 1;
    for (StudyItem lesson : items) {
      if (lesson instanceof Lesson) {
        lesson.setIndex(index);
        index++;
        lesson.init(course, this, isRestarted);
      }
    }
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public void setCourse(int course) {
    this.course = course;
  }

  public void setPosition(int position) {
    this.position = position;
  }

  public int getCourse() {
    return course;
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

  public boolean isUpToDate() {
    if (id == 0) return true;
    Section section = StepikConnector.getSection(id);
    if (section.getUpdateDate() == null) return true;
    if (myUpdateDate == null) return false;
    if (section.units.size() != getLessons().size()) return false;
    for (Lesson lesson : getLessons()) {
      if (!lesson.isUpToDate()) {
        return false;
      }
    }

    return !section.getUpdateDate().after(myUpdateDate);
  }

  public void setUpdateDate(Date updateDate) {
    myUpdateDate = updateDate;
  }

  public Date getUpdateDate() {
    return myUpdateDate;
  }
}
