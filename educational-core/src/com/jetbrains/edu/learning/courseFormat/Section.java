package com.jetbrains.edu.learning.courseFormat;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class Section {
  public List<Integer> units;
  private int course;
  @Expose private String title;
  private int position;
  private int id;

  @Expose @SerializedName("lessons") public List<Integer> lessonIndexes = new ArrayList<>();

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public void setCourse(int course) {
    this.course = course;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getTitle() {
    return title;
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
}
