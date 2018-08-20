package com.jetbrains.edu.learning.checkio.newProject;

import com.intellij.openapi.progress.ProgressManager;
import com.jetbrains.edu.learning.checkio.CheckiOCourseContentGenerator;
import com.jetbrains.edu.learning.checkio.api.exceptions.ApiException;
import com.jetbrains.edu.learning.checkio.connectors.CheckiOApiConnector;
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOCourse;
import com.jetbrains.edu.learning.checkio.exceptions.CheckiOLoginRequiredException;
import org.jetbrains.annotations.NotNull;

public final class CheckiOCourseGeneratingUtils {
  private CheckiOCourseGeneratingUtils() {}

  public static CheckiOCourse generateCourseFromServer(
    @NotNull CheckiOCourseContentGenerator courseContentGenerator,
    @NotNull CheckiOApiConnector apiConnector
  ) throws CheckiOLoginRequiredException, ApiException {
    return courseContentGenerator.generateCourseFromMissions(apiConnector.getMissionList());
  }

  public static CheckiOCourse generateCourseFromServerUnderProgress(
    @NotNull CheckiOCourseContentGenerator courseContentGenerator,
    @NotNull CheckiOApiConnector apiConnector
  ) throws Exception {
    return ProgressManager.getInstance().runProcessWithProgressSynchronously(
      () -> generateCourseFromServer(courseContentGenerator, apiConnector),
      "Getting Course from Server",
      false,
      null
    );
  }
}
