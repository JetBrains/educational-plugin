package com.jetbrains.edu.learning.courseFormat;

import com.jetbrains.edu.learning.courseFormat.visitors.LessonVisitor;
import com.jetbrains.edu.learning.courseFormat.visitors.SectionVisitor;
import com.jetbrains.edu.learning.courseFormat.visitors.TaskVisitor;
import one.util.streamex.StreamEx;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public abstract class LessonContainer extends ItemContainer {
  @Nullable
  public Lesson getLesson(@NotNull final String name) {
    return getLesson(lesson -> name.equals(lesson.getName()));
  }

  @Nullable
  public Lesson getLesson(int id) {
    return getLesson(lesson -> id == lesson.getId());
  }

  @Nullable
  public Lesson getLesson(Predicate<Lesson> isLesson) {
    return (Lesson)StreamEx.of(items).filter(Lesson.class::isInstance)
      .findFirst(item -> isLesson.test((Lesson)item)).orElse(null);
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

  public void visitLessons(@NotNull LessonVisitor visitor) {
    for (StudyItem item : items) {
      if (item instanceof Lesson) {
        visitor.visit((Lesson)item);
      }
      else if (item instanceof Section) {
        for (Lesson lesson : ((Section)item).getLessons()) {
          visitor.visit(lesson);
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

  public void visitTasks(@NotNull TaskVisitor visitor) {
    visitLessons(lesson -> lesson.visitTasks(visitor));
  }
}
