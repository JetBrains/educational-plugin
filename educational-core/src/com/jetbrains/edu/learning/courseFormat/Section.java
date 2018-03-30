package com.jetbrains.edu.learning.courseFormat;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.jetbrains.edu.learning.EduUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Section extends StudyItem {
  public List<Integer> units;
  private int course;
  @Expose
  @SerializedName("title")
  private String name;

  private int position;
  private int id;

  @Expose @SerializedName("lessons") private List<Lesson> lessons = new ArrayList<>();

  public void initSection(Course course, boolean isRestarted) {
    for (Lesson lesson : lessons) {
      lesson.initLesson(course, this, isRestarted);
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

  @Nullable
  public Lesson getLesson(@NotNull final String name) {
    return lessons.stream().filter(item -> item.getName().equals(name)).findFirst().orElse(null);
  }

  public List<Lesson> getLessons() {
    return lessons;
  }

  public void setLessons(List<Lesson> lessons) {
    this.lessons = lessons;
  }

  public void addLessons(@NotNull final List<Lesson> lessons) {
    this.lessons.addAll(lessons);
  }

  public void addLesson(@NotNull final Lesson lesson) {
    this.lessons.add(lesson);
  }

  public void removeLesson(Lesson lesson) {
    lessons.remove(lesson);
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public void setName(String name) {
    this.name = name;
  }

  public void sortLessons() {
    Collections.sort(lessons, EduUtils.INDEX_COMPARATOR);
  }
}
