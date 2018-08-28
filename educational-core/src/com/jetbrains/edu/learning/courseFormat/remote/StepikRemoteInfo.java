package com.jetbrains.edu.learning.courseFormat.remote;

import com.intellij.openapi.project.Project;
import com.jetbrains.edu.learning.EduNames;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.Lesson;
import com.jetbrains.edu.learning.stepik.courseFormat.StepikCourse;
import com.jetbrains.edu.learning.courseFormat.Tag;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.stepik.StepikAdaptiveReactionsPanel;
import com.jetbrains.edu.learning.stepik.StepikUtils;
import org.fest.util.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class StepikRemoteInfo implements RemoteInfo {
  // publish to stepik
  private boolean isPublic;
  private boolean isAdaptive = false;
  private boolean isIdeaCompatible = true;
  private Date myUpdateDate = new Date(0);
  private int myId;
  private List<Integer> mySectionIds = new ArrayList<>(); // in CC mode is used to store top-level lessons section id
  private List<Integer> myInstructors = new ArrayList<>();

  // do not publish to stepik
  private boolean myLoadSolutions = true; // disabled for reset courses
  private Date myAdditionalMaterialsUpdateDate = new Date(0);

  @Override
  public boolean isCourseValid(@NotNull Course course) {
    if (isAdaptive) {
      final List<Lesson> lessons = course.getLessons();
      if (lessons.size() == 1) {
        return !lessons.get(0).getTaskList().isEmpty();
      }
    }
    return true;
  }

  @NotNull
  @Override
  public String wrapTaskText(@NotNull final String taskText, @NotNull final Task task) {
    final Course course = task.getLesson().getCourse();
    return course instanceof StepikCourse ? StepikUtils.wrapStepikTasks(task, taskText, (StepikCourse)course) : taskText;
  }

  @NotNull
  @Override
  public List<Tag> getTags() {
    if (isAdaptive) {
      return Lists.newArrayList(new Tag(EduNames.ADAPTIVE));
    }
    return Lists.emptyList();
  }

  @Nullable
  @Override
  public JPanel getAdditionalDescriptionPanel(Project project) {
    return isAdaptive ? new StepikAdaptiveReactionsPanel(project) : null;
  }

  public boolean isPublic() {
    return isPublic;
  }

  public void setPublic(boolean isPublic) {
    this.isPublic = isPublic;
  }

  public boolean isAdaptive() {
    return isAdaptive;
  }

  public void setAdaptive(boolean adaptive) {
    isAdaptive = adaptive;
  }

  public boolean isIdeaCompatible() {
    return isIdeaCompatible;
  }

  public void setIdeaCompatible(boolean compatible) {
    isIdeaCompatible = compatible;
  }

  public int getId() {
    return myId;
  }

  public void setId(int id) {
    myId = id;
  }

  public void setUpdateDate(Date date) {
    myUpdateDate = date;
  }

  public Date getUpdateDate() {
    return myUpdateDate;
  }

  public List<Integer> getSectionIds() {
    return mySectionIds;
  }

  public void setSectionIds(List<Integer> sectionIds) {
    this.mySectionIds = sectionIds;
  }

  public void setInstructors(List<Integer> instructors) {
    myInstructors = instructors;
  }

  public List<Integer> getInstructors() {
    return myInstructors;
  }

  public boolean isLoadSolutions() {
    return myLoadSolutions;
  }

  public void setLoadSolutions(boolean myLoadSolutions) {
    this.myLoadSolutions = myLoadSolutions;
  }

  public Date getAdditionalMaterialsUpdateDate() {
    return myAdditionalMaterialsUpdateDate;
  }

  public void setAdditionalMaterialsUpdateDate(@NotNull Date additionalMaterialsUpdateDate) {
    myAdditionalMaterialsUpdateDate = additionalMaterialsUpdateDate;
  }
}
