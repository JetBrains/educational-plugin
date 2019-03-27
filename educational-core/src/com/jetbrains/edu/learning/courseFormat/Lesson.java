package com.jetbrains.edu.learning.courseFormat;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.xmlb.annotations.AbstractCollection;
import com.intellij.util.xmlb.annotations.Transient;
import com.jetbrains.edu.learning.OpenApiExtKt;
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOMission;
import com.jetbrains.edu.learning.courseFormat.tasks.*;
import kotlin.collections.CollectionsKt;
import one.util.streamex.StreamEx;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * To introduce new lesson type it's required to:
 *  - Extend Lesson class
 *  - Go to {@link ItemContainer#items} and update elementTypes in AbstractCollection annotation. Needed for proper xml serialization
 *  - Handle xml migration in {@link com.jetbrains.edu.learning.serialization.converter.xml.BaseXmlConverter#convert}
 *  - Handle yaml deserialization {@link com.jetbrains.edu.coursecreator.yaml.YamlDeserializer#deserializeLesson(String)}
 */
public class Lesson extends StudyItem {
  @Transient public List<Integer> steps;
  @Transient boolean is_public;
  private Date myUpdateDate = new Date(0);

  @SuppressWarnings("deprecation")
  @AbstractCollection(elementTypes = {
    CheckiOMission.class,
    EduTask.class,
    ChoiceTask.class,
    TheoryTask.class,
    CodeTask.class,
    OutputTask.class,
    IdeTask.class
  })
  public List<Task> taskList = new ArrayList<>();

  @Transient
  private Course myCourse = null;

  public int unitId = 0;

  @Transient
  private Section mySection = null;

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

  public List<Task> getTaskList() {
    return taskList;
  }

  public List<Task> getTaskListForProgress() {
    return CollectionsKt.filter(taskList, task -> !(task instanceof TheoryTask));
  }

  @NotNull
  @Transient
  public Course getCourse() {
    return myCourse;
  }

  @NotNull
  @Override
  public StudyItem getParent() {
    return mySection == null ? myCourse : mySection;
  }

  @Transient
  public void setCourse(Course course) {
    myCourse = course;
  }

  public void addTask(@NotNull final Task task) {
    taskList.add(task);
  }

  @Nullable
  public Task getTask(@NotNull final String name) {
    return StreamEx.of(taskList).findFirst(task -> name.equals(task.getName())).orElse(null);
  }

  public Task getTask(int id) {
    for (Task task : taskList) {
      if (task.getId() == id) {
        return task;
      }
    }
    return null;
  }

  public void updateTaskList(List<Task> taskList) {
    this.taskList = taskList;
  }

  public CheckStatus getStatus() {
    for (Task task : taskList) {
      if (task.getStatus() != CheckStatus.Solved) {
        return CheckStatus.Unchecked;
      }
    }
    return CheckStatus.Solved;
  }

  public Date getUpdateDate() {
    return myUpdateDate;
  }

  public void setUpdateDate(Date updateDate) {
    myUpdateDate = updateDate;
  }

  @Transient
  public boolean isPublic() {
    return is_public;
  }

  @Transient
  public void setPublic(boolean isPublic) {
    this.is_public = isPublic;
  }

  @Transient
  @Nullable
  public Section getSection() {
    return mySection;
  }

  @Transient
  public void setSection(@Nullable Section section) {
    mySection = section;
  }

  @Nullable
  public VirtualFile getLessonDir(@NotNull final Project project) {
    VirtualFile courseDir = OpenApiExtKt.getCourseDir(project);

    if (mySection == null) {
      return courseDir.findChild(getName());
    }
    else {
      VirtualFile sectionDir = courseDir.findChild(mySection.getName());
      assert sectionDir != null : "Section dir for lesson not found";

      return sectionDir.findChild(getName());
    }
  }

  @NotNull
  public ItemContainer getContainer() {
    if (mySection != null) {
      return mySection;
    }
    return myCourse;
  }

  @Override
  @Nullable
  public VirtualFile getDir(@NotNull Project project) {
    return getLessonDir(project);
  }

  public void visitTasks(@NotNull TaskVisitor visitor) {
    int index = 1;
    for (Task task : taskList) {
      boolean visitNext = visitor.visit(task, index);
      if (!visitNext) {
        return;
      }
      index++;
    }
  }
}
