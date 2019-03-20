package com.jetbrains.edu.learning.courseFormat;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.xmlb.annotations.Transient;
import com.jetbrains.edu.learning.EduVersions;
import com.jetbrains.edu.learning.configuration.EduConfiguratorManager;
import com.jetbrains.edu.learning.stepik.StepikNames;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class EduCourse extends Course {
  private static final Logger LOG = Logger.getInstance(Course.class);

  // Fields from stepik:
  private boolean isCompatible = true;
  //course type in format "pycharm<version> <language>"
  private String myType =
    String.format("%s%d %s", StepikNames.PYCHARM_PREFIX, EduVersions.JSON_FORMAT_VERSION, getLanguage());
  // in CC mode is used to store top-level lessons section id
  List<Integer> sectionIds = new ArrayList<>();
  List<Integer> instructors = new ArrayList<>();
  private int id;
  private Date myUpdateDate = new Date(0);
  private Date myCreateDate = new Date(0);
  boolean isPublic;
  @Transient private String myAdminsGroup;

  // Not published to stepik:
  private boolean myLoadSolutions = true; // disabled for reset courses

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

  public void setUpdateDate(Date date) {
    myUpdateDate = date;
  }

  public Date getUpdateDate() {
    return myUpdateDate;
  }

  public Date getCreateDate() { return myCreateDate; }

  public void setCreateDate(Date createDate) { myCreateDate = createDate; }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
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

  @Transient
  public String getAdminsGroup() {
    return myAdminsGroup;
  }

  @Transient
  public void setAdminsGroup(String adminsGroup) {
    myAdminsGroup = adminsGroup;
  }

  @NotNull
  private static CourseCompatibility courseCompatibility(@NotNull EduCourse courseInfo) {
    final List<String> supportedLanguages = EduConfiguratorManager.getSupportedEduLanguages();

    String courseType = courseInfo.getType();
    final List<String> typeLanguage = StringUtil.split(courseType, " ");
    if (typeLanguage.size() < 2) {
      return CourseCompatibility.UNSUPPORTED;
    }
    String prefix = typeLanguage.get(0);
    if (!supportedLanguages.contains(courseInfo.getLanguageID())) return CourseCompatibility.UNSUPPORTED;
    if (!prefix.startsWith(StepikNames.PYCHARM_PREFIX)) {
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
      }
      else {
        return CourseCompatibility.INCOMPATIBLE_VERSION;
      }
    }
    catch (NumberFormatException e) {
      LOG.info("Wrong version format", e);
      return CourseCompatibility.UNSUPPORTED;
    }
  }

  public boolean isRemote() {
    return id != 0;
  }

  public void convertToLocal() {
    isPublic = false;
    isCompatible = true;
    id = 0;
    myUpdateDate = new Date(0);
    myCreateDate = new Date(0);
    sectionIds = new ArrayList<>();
    instructors = new ArrayList<>();
    myType = String.format("%s%d %s", StepikNames.PYCHARM_PREFIX, EduVersions.JSON_FORMAT_VERSION, getLanguage());
    myLoadSolutions = true;
  }
}
