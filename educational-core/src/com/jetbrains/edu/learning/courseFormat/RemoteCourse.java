package com.jetbrains.edu.learning.courseFormat;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task.Backgroundable;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.KeyedLazyInstance;
import com.jetbrains.edu.learning.EduConfiguratorManager;
import com.jetbrains.edu.learning.EduVersions;
import com.jetbrains.edu.learning.EduSettings;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.stepik.StepikConnector;
import com.jetbrains.edu.learning.stepik.StepikNames;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class RemoteCourse extends Course {

  private static final Logger LOG = Logger.getInstance(Course.class);

  private static List<String> ourSupportedLanguages;

  //course type in format "pycharm<version> <language>"
  @SerializedName("course_format") private String myType =
                        String.format("%s%d %s", StepikNames.PYCHARM_PREFIX, EduVersions.JSON_FORMAT_VERSION, getLanguageID());
  @SerializedName("is_idea_compatible") private boolean isCompatible = true;
  @SerializedName("sections") List<Integer> sectionIds;
  List<Integer> instructors = new ArrayList<>();
  @Expose private int id;
  @Expose @SerializedName("update_date") private Date myUpdateDate;
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


    RemoteCourse courseFromServer = StepikConnector.getCourseFromStepik(EduSettings.getInstance().getUser(), id, isCompatible);
    if (courseFromServer == null) return true;

    final Date date = courseFromServer.getUpdateDate();
    if (date == null) return true;
    if (date.after(myUpdateDate)) {
      return false;
    }

    int itemsWithoutAdditional = courseFromServer.sectionIds.size() - 1;
    int itemsSize = getLessons(false).size() + getSections().size();
    if (itemsSize < itemsWithoutAdditional) {
      return false;
    }

    for (StudyItem item : items) {
      if (item instanceof Section) {
        if (!((Section)item).isUpToDate()) {
          return false;
        }
      }
      else if (item instanceof Lesson) {
        if (!((Lesson)item).isUpToDate()) {
          return false;
        }
      }
    }

    return true;
  }

  @Override
  public void setUpdated() {
    setUpdateDate(StepikConnector.getCourseUpdateDate(id));
    visitLessons((lesson) -> {
      lesson.setUpdateDate(StepikConnector.getLessonUpdateDate(lesson.getId()));
      for (Task task : lesson.getTaskList()) {
        task.setUpdateDate(StepikConnector.getTaskUpdateDate(task.getStepId()));
      }
      return true;
    });

    visitSections((section) -> {
      section.setUpdateDate(StepikConnector.getSectionUpdateDate((section).getId()));
      return true;
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
      version = String.valueOf(EduVersions.JSON_FORMAT_VERSION);
    }
    else {
      version = myType.substring(StepikNames.PYCHARM_PREFIX.length(), separator);
    }

    setType(String.format("%s%s %s", StepikNames.PYCHARM_PREFIX, version, language));
  }

  public void setType(String type) {
    myType = type;
    myCompatibility = courseCompatibility(this);
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

  @NotNull
  private static List<String> getSupportedLanguages() {
    if (ourSupportedLanguages == null) {
      final List<String> supportedLanguages = EduConfiguratorManager.allExtensions()
        .stream()
        .map(KeyedLazyInstance::getKey)
        .collect(Collectors.toList());
      ourSupportedLanguages = supportedLanguages;
      return supportedLanguages;
    } else {
      return ourSupportedLanguages;
    }
  }

  @NotNull
  private static CourseCompatibility courseCompatibility(@NotNull RemoteCourse courseInfo) {
    final List<String> supportedLanguages = getSupportedLanguages();

    if (courseInfo.isAdaptive()) {
      if (supportedLanguages.contains(courseInfo.getLanguageID())) {
        return CourseCompatibility.COMPATIBLE;
      } else {
        return CourseCompatibility.UNSUPPORTED;
      }
    }

    String courseType = courseInfo.getType();
    final List<String> typeLanguage = StringUtil.split(courseType, " ");
    String prefix = typeLanguage.get(0);
    if (!supportedLanguages.contains(courseInfo.getLanguageID())) return CourseCompatibility.UNSUPPORTED;
    if (typeLanguage.size() < 2 || !prefix.startsWith(StepikNames.PYCHARM_PREFIX)) {
      return CourseCompatibility.UNSUPPORTED;
    }
    String versionString = prefix.substring(StepikNames.PYCHARM_PREFIX.length());
    if (versionString.isEmpty()) {
      return CourseCompatibility.COMPATIBLE;
    }
    try {
      Integer version = Integer.valueOf(versionString);
      if (version <= EduVersions.JSON_FORMAT_VERSION) {
        return CourseCompatibility.COMPATIBLE;
      } else {
        return CourseCompatibility.INCOMPATIBLE_VERSION;
      }
    }
    catch (NumberFormatException e) {
      LOG.info("Wrong version format", e);
      return CourseCompatibility.UNSUPPORTED;
    }
  }
}
