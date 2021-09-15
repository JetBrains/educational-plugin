package com.jetbrains.edu.learning.courseFormat;

import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.courseFormat.visitors.TaskVisitor;
import com.jetbrains.edu.learning.yaml.YamlDeserializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;

/**
 * To introduce new lesson type it's required to:
 *  - Extend Lesson class
 *  - Handle yaml deserialization {@link YamlDeserializer#deserializeLesson(com.fasterxml.jackson.databind.ObjectMapper, String)}
 */
public class Lesson extends ItemContainer {
  transient public List<Integer> steps;
  transient boolean is_public;
  public int unitId = 0;

  transient private Course myCourse = null;
  transient private Section mySection = null;

  public Lesson() {
  }

  public void init(@Nullable final Course course, @Nullable final StudyItem section, boolean isRestarted) {
    mySection = section instanceof Section ? (Section)section : null;
    setCourse(course);
    List<Task> tasks = getTaskList();
    for (int i = 0; i < tasks.size(); i++) {
      Task task = tasks.get(i);
      task.setIndex(i + 1);
      task.init(course, this, isRestarted);
    }
  }

  /**
   * Returns tasks copy. Dedicated methods should be used to modify list of lesson items ([addTask], [removeTask])
   */
  public List<Task> getTaskList() {
    return items.stream().filter(Task.class::isInstance).map(Task.class::cast).collect(Collectors.toList());
  }

  @NotNull
  public Course getCourse() {
    return myCourse;
  }

  @NotNull
  @Override
  public StudyItem getParent() {
    return mySection == null ? myCourse : mySection;
  }

  @Override
  public String getItemType() {
    return "lesson";
  }

  public void setCourse(Course course) {
    myCourse = course;
  }

  public void addTask(@NotNull final Task task) {
    items.add(task);
  }

  public void addTask(int index, @NotNull final Task task) {
    items.add(index, task);
  }

  public void removeTask(@NotNull final Task task) {
    items.remove(task);
  }

  @Nullable
  public Task getTask(@NotNull final String name) {
    return (Task)getItem(name);
  }

  @Nullable
  public Task getTask(int id) {
    for (Task task : getTaskList()) {
      if (task.getId() == id) {
        return task;
      }
    }
    return null;
  }

  public boolean isPublic() {
    return is_public;
  }

  public void setPublic(boolean isPublic) {
    this.is_public = isPublic;
  }

  @Nullable
  public Section getSection() {
    return mySection;
  }

  public void setSection(@Nullable Section section) {
    mySection = section;
  }

  @NotNull
  public LessonContainer getContainer() {
    if (mySection != null) {
      return mySection;
    }
    return myCourse;
  }

  @Override
  @Nullable
  public VirtualFile getDir(@NotNull final VirtualFile baseDir) {
    if (mySection == null) {
      return baseDir.findChild(getName());
    }
    else {
      VirtualFile sectionDir = baseDir.findChild(mySection.getName());
      assert sectionDir != null : "Section dir for lesson not found";

      return sectionDir.findChild(getName());
    }
  }

  public void visitTasks(@NotNull TaskVisitor visitor) {
    for (Task task : getTaskList()) {
      visitor.visit(task);
    }
  }
}
