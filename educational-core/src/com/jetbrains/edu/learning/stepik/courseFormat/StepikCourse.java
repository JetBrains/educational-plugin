package com.jetbrains.edu.learning.stepik.courseFormat;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.text.StringUtil;
import com.jetbrains.edu.learning.EduConfiguratorManager;
import com.jetbrains.edu.learning.EduVersions;
import com.jetbrains.edu.learning.courseFormat.*;
import com.jetbrains.edu.learning.courseFormat.remote.RemoteInfo;
import com.jetbrains.edu.learning.stepik.StepikNames;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class StepikCourse extends Course {
  private static final Logger LOG = Logger.getInstance(Course.class);

  public StepikCourse() {
    setRemoteInfo(new StepikCourseRemoteInfo());
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

  public void updateCourseCompatibility() {
    final StepikCourseRemoteInfo info = getStepikRemoteInfo();
    final List<String> supportedLanguages = EduConfiguratorManager.getSupportedLanguages();

    final List<String> typeLanguage = StringUtil.split(info.getCourseFormat(), " ");
    String prefix = typeLanguage.get(0);
    if (!supportedLanguages.contains(getLanguageID())) myCompatibility = CourseCompatibility.UNSUPPORTED;
    if (typeLanguage.size() < 2 || !prefix.startsWith(StepikNames.PYCHARM_PREFIX)) {
      myCompatibility =  CourseCompatibility.UNSUPPORTED;
      return;
    }
    String versionString = prefix.substring(StepikNames.PYCHARM_PREFIX.length());
    if (versionString.isEmpty()) {
      myCompatibility = CourseCompatibility.COMPATIBLE;
      return;
    }
    try {
      Integer version = Integer.valueOf(versionString);
      if (version <= EduVersions.JSON_FORMAT_VERSION) {
        myCompatibility = CourseCompatibility.COMPATIBLE;
      } else {
        myCompatibility = CourseCompatibility.INCOMPATIBLE_VERSION;
      }
    }
    catch (NumberFormatException e) {
      LOG.info("Wrong version format", e);
      myCompatibility = CourseCompatibility.UNSUPPORTED;
    }
  }

  public StepikCourseRemoteInfo getStepikRemoteInfo() {
    final RemoteInfo info = super.getRemoteInfo();
    assert info instanceof StepikCourseRemoteInfo;
    return (StepikCourseRemoteInfo)info;
  }
}
