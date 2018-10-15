package com.jetbrains.edu.learning.stepik.courseFormat;

import com.google.gson.annotations.SerializedName;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.KeyedLazyInstance;
import com.jetbrains.edu.learning.EduConfiguratorManager;
import com.jetbrains.edu.learning.EduVersions;
import com.jetbrains.edu.learning.courseFormat.*;
import com.jetbrains.edu.learning.courseFormat.remote.RemoteInfo;
import com.jetbrains.edu.learning.stepik.StepikNames;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

public class StepikCourse extends Course {
  private static final Logger LOG = Logger.getInstance(Course.class);

  private static List<String> ourSupportedLanguages;

  //course type in format "pycharm<version> <language>"
  @SerializedName("course_format") private String myType =
                        String.format("%s%d %s", StepikNames.PYCHARM_PREFIX, EduVersions.JSON_FORMAT_VERSION, getLanguageID());

  public StepikCourse() {
    setRemoteInfo(new StepikCourseRemoteInfo());
  }

  public String getType() {
    return myType;
  }

  @Override
  public void setLanguage(@NotNull final String language) {
    super.setLanguage(language);
    updateType(language);
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
    myCompatibility = courseCompatibility();
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
  private CourseCompatibility courseCompatibility() {
    final List<String> supportedLanguages = getSupportedLanguages();

    final List<String> typeLanguage = StringUtil.split(myType, " ");
    String prefix = typeLanguage.get(0);
    if (!supportedLanguages.contains(getLanguageID())) return CourseCompatibility.UNSUPPORTED;
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

  public StepikCourseRemoteInfo getStepikRemoteInfo() {
    final RemoteInfo info = super.getRemoteInfo();
    assert info instanceof StepikCourseRemoteInfo;
    return (StepikCourseRemoteInfo)info;
  }
}
