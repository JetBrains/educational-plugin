package com.jetbrains.edu.learning.checkio.utils;

import com.intellij.openapi.diagnostic.Logger;
import com.jetbrains.edu.learning.checkio.CheckiOCourseContentGenerator;
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOCourse;
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOStation;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class CheckiOCourseGenerationUtils {
  private static final Logger LOG = Logger.getInstance(CheckiOCourseGenerationUtils.class);

  public static boolean generateCourseFromServerUnderProgress(
    @NotNull CheckiOCourseContentGenerator contentGenerator,
    @NotNull CheckiOCourse course
  ) {
    try {
      final List<CheckiOStation> stations = contentGenerator.getStationsFromServerUnderProgress();
      stations.forEach(course::addStation);
      return true;
    }
    catch (Exception e) {
      // Notifications aren't able to be shown during course generating process,
      // so we just log the error and return false
      LOG.warn(e);
      return false;
    }
  }
}
