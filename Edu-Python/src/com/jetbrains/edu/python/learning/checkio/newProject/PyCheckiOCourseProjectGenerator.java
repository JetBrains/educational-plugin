package com.jetbrains.edu.python.learning.checkio.newProject;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.edu.learning.checkio.newProject.CheckiOCourseGeneratingUtils;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.python.learning.PyCourseBuilder;
import com.jetbrains.edu.python.learning.checkio.connectors.PyCheckiOApiConnector;
import com.jetbrains.edu.python.learning.newproject.PyCourseProjectGenerator;
import org.jetbrains.annotations.NotNull;

public class PyCheckiOCourseProjectGenerator extends PyCourseProjectGenerator {
  private static final Logger LOG = Logger.getInstance(PyCheckiOCourseProjectGenerator.class);

  public PyCheckiOCourseProjectGenerator(@NotNull PyCourseBuilder builder,
                                         @NotNull Course course) {
    super(builder, course);
  }

  @Override
  protected void createAdditionalFiles(@NotNull Project project, @NotNull VirtualFile baseDir) {

  }

  @Override
  protected boolean beforeProjectGenerated() {
    try {
      myCourse = CheckiOCourseGeneratingUtils.generateCourseFromServerUnderProgress(
        PyCheckiOCourseContentGenerator.getInstance(),
        PyCheckiOApiConnector.getInstance()
      );
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
