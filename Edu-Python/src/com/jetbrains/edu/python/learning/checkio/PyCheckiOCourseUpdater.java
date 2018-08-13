package com.jetbrains.edu.python.learning.checkio;

import com.intellij.openapi.project.Project;
import com.jetbrains.edu.learning.checkio.CheckiOCourseUpdater;
import com.jetbrains.edu.learning.checkio.api.exceptions.ApiException;
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOCourse;
import com.jetbrains.edu.learning.checkio.exceptions.LoginRequiredException;
import com.jetbrains.edu.python.learning.checkio.connectors.PyCheckiOApiConnector;
import org.jetbrains.annotations.NotNull;

public class PyCheckiOCourseUpdater extends CheckiOCourseUpdater {
  public PyCheckiOCourseUpdater(@NotNull CheckiOCourse course,
                                @NotNull Project project) {
    super(course, project);
  }

  @NotNull
  @Override
  protected CheckiOCourse getCourseFromServer() throws LoginRequiredException, ApiException {
    return PyCheckiOCourseContentGenerator.getInstance().generateCourseFromMissions(PyCheckiOApiConnector.getInstance().getMissionList());
  }
}
