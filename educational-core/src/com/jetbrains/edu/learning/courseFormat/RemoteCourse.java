package com.jetbrains.edu.learning.courseFormat;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task.Backgroundable;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.stepik.StepikConnector;
import com.jetbrains.edu.learning.stepik.StepikNames;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class RemoteCourse extends Course {
  //course type in format "pycharm<version> <language>"
  @SerializedName("course_format") private String myType =
                        String.format("%s%d %s", StepikNames.PYCHARM_PREFIX, StepikConnector.CURRENT_VERSION, getLanguageID());
  @SerializedName("is_idea_compatible") private boolean isCompatible = true;
  @SerializedName("sections") List<Integer> sectionIds;
  List<Integer> instructors = new ArrayList<>();
  @Expose private int id;
  @Expose @SerializedName("update_date") private Date myUpdateDate;
  private Boolean isUpToDate = true;
  @Expose private boolean isAdaptive = false;
  @Expose @SerializedName("is_public") boolean isPublic;
  @Expose private boolean myLoadSolutions = true; // disabled for reset courses

  public String getType() {
    return myType;
  }

  @Override
  public void setLanguage(@NotNull final String language) {
    super.setLanguage(language);
    updateType(language);
  }

  public List<Integer> getSectionIds() {
    return sectionIds;
  }

  public void setSectionIds(List<Integer> sectionIds) {
    this.sectionIds = sectionIds;
  }

  public void setInstructors(List<Integer> instructors) {
    this.instructors = instructors;
  }

  public List<Integer> getInstructors() {
    return instructors;
  }

  @NotNull
  @Override
  public List<Tag> getTags() {
    final List<Tag> tags = super.getTags();
    if (getVisibility() instanceof CourseVisibility.FeaturedVisibility) {
      tags.add(new FeaturedTag());
    }
    if (getVisibility() instanceof CourseVisibility.InProgressVisibility) {
      tags.add(new InProgressTag());
    }
    return tags;
  }

  @Override
  public boolean isUpToDate() {
    if (id == 0) return true;
    if (!isStudy()) return true;

    ProgressManager.getInstance().runProcessWithProgressAsynchronously(new Backgroundable(null, "Updating Course") {
      @Override
      public void run(@NotNull ProgressIndicator indicator) {
        final Date date = StepikConnector.getCourseUpdateDate(id);
        if (date == null) return;
        if (date.after(myUpdateDate)) {
          isUpToDate = false;
        }
        visitLessons(new LessonVisitor() {
          @Override
          public boolean visitLesson(@NotNull Lesson lesson, int index) {
            if (!lesson.isUpToDate()) {
              isUpToDate = false;
            }
            return true;
          }
        });
      }
    }, new EmptyProgressIndicator());

    return isUpToDate;
  }

  @Override
  public void setUpdated() {
    setUpdateDate(StepikConnector.getCourseUpdateDate(id));
    visitLessons(new LessonVisitor() {
      @Override
      public boolean visitLesson(@NotNull Lesson lesson, int index) {
        lesson.setUpdateDate(StepikConnector.getLessonUpdateDate(lesson.getId()));
        for (Task task : lesson.getTaskList()) {
          task.setUpdateDate(StepikConnector.getTaskUpdateDate(task.getStepId()));
        }
        return true;
      }
    });
  }

  public void setUpdateDate(Date date) {
    myUpdateDate = date;
  }

  public Date getUpdateDate() {
    return myUpdateDate;
  }

  @Override
  public boolean isAdaptive() {
    return isAdaptive;
  }

  public void setAdaptive(boolean adaptive) {
    isAdaptive = adaptive;
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public void copyCourseParameters(RemoteCourse course) {
    setName(course.getName());
    setUpdateDate(course.getUpdateDate());
  }

  private void updateType(String language) {
    final int separator = myType.indexOf(" ");
    final String version;
    if (separator == -1) {
      version = String.valueOf(StepikConnector.CURRENT_VERSION);
    }
    else {
      version = myType.substring(StepikNames.PYCHARM_PREFIX.length(), separator);
    }

    myType = String.format("%s%s %s", StepikNames.PYCHARM_PREFIX, version, language);
  }

  public void setType(String type) {
    myType = type;
  }

  public boolean isPublic() {
    return isPublic;
  }

  public void setPublic(boolean isPublic) {
    this.isPublic = isPublic;
  }

  public boolean isLoadSolutions() {
    return myLoadSolutions;
  }

  public void setLoadSolutions(boolean myLoadSolutions) {
    this.myLoadSolutions = myLoadSolutions;
  }

  public boolean isCompatible() {
    return isCompatible;
  }

  public void setCompatible(boolean compatible) {
    isCompatible = compatible;
  }
}
