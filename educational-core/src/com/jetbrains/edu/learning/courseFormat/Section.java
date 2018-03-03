package com.jetbrains.edu.learning.courseFormat;

import java.util.ArrayList;
import java.util.List;

public class Section {
  public List<Integer> units;
  private int course;
  private String title;
  private int position;
  private int id;

  public List<Integer> lessonIds = new ArrayList<>();

  public int getId() {
    return id;
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
}
