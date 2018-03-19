package com.jetbrains.edu.learning.courseFormat;

import com.google.gson.annotations.Expose;
import com.intellij.util.xmlb.annotations.AbstractCollection;
import com.jetbrains.edu.learning.EduUtils;
import one.util.streamex.StreamEx;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public abstract class ItemContainer extends StudyItem {
  @AbstractCollection(elementTypes = {
    Section.class,
    Lesson.class,
    FrameworkLesson.class
  })
  @Expose protected List<StudyItem> items = new ArrayList<>();

  @Nullable
  public Lesson getLesson(@NotNull final String name) {
    return (Lesson)StreamEx.of(items).filter(Lesson.class::isInstance)
      .findFirst(lesson -> name.equals(lesson.getName())).orElse(null);
  }

  @Nullable
  public Lesson getLesson(int id) {
    return (Lesson)StreamEx.of(items).filter(Lesson.class::isInstance)
      .findFirst(item -> id == ((Lesson)item).getId()).orElse(null);
  }

  @Nullable
  public StudyItem getItem(@NotNull final String name) {
    return items.stream().filter(item -> item.getName().equals(name)).findFirst().orElse(null);
  }

  @NotNull
  public List<StudyItem> getItems() {
    return Collections.unmodifiableList(items);
  }

  @NotNull
  public List<Lesson> getLessons() {
    return items.stream().filter(Lesson.class::isInstance).map(Lesson.class::cast).collect(Collectors.toList());
  }

  public void addLessons(@NotNull final List<Lesson> lessons) {
    items.addAll(lessons);
  }

  public void addLesson(@NotNull final Lesson lesson) {
    items.add(lesson);
  }

  public void removeLesson(@NotNull Lesson lesson) {
    items.remove(lesson);
  }

  public void sortItems() {
    Collections.sort(items, EduUtils.INDEX_COMPARATOR);
  }

  public void visitLessons(@NotNull LessonVisitor visitor) {
    for (StudyItem item : items) {
      if (item instanceof Lesson) {
        final boolean visitNext = visitor.visit((Lesson)item);
        if (!visitNext) {
          return;
        }
      }
      else if (item instanceof Section) {
        for (Lesson lesson : ((Section)item).getLessons()) {
          final boolean visitNext = visitor.visit(lesson);
          if (!visitNext) {
            return;
          }
        }
      }
    }
  }

  public void visitSections(@NotNull SectionVisitor visitor) {
    for (StudyItem item : items) {
      if (item instanceof Section) {
        visitor.visit((Section)item);
      }
    }
  }
}
