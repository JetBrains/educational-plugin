package com.jetbrains.edu.learning.courseFormat;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class LessonContainer extends StudyItem {

  @Nullable
  public Lesson getLesson(@NotNull final String name) {
    return getLessons().stream().filter(item -> item.getName().equals(name)).findFirst().orElse(null);
  }

  public abstract List<Lesson> getLessons();

  public abstract void addLessons(@NotNull final List<Lesson> lessons);

  public abstract void addLesson(@NotNull final Lesson lesson);

  public abstract void removeLesson(Lesson lesson);

  public abstract void sortChildren();
}
