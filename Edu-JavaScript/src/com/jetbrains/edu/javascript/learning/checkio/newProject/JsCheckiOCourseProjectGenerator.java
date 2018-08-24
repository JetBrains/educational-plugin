package com.jetbrains.edu.javascript.learning.checkio.newProject;

import com.intellij.lang.javascript.JavaScriptFileType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.edu.javascript.learning.JsNewProjectSettings;
import com.jetbrains.edu.javascript.learning.checkio.JsCheckiOCourseBuilder;
import com.jetbrains.edu.javascript.learning.checkio.connectors.JsCheckiOApiConnector;
import com.jetbrains.edu.learning.checkio.CheckiOCourseContentGenerator;
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOCourse;
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOStation;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class JsCheckiOCourseProjectGenerator extends CourseProjectGenerator<JsNewProjectSettings> {
  private static final Logger LOG = Logger.getInstance(JsCheckiOCourseProjectGenerator.class);

  public JsCheckiOCourseProjectGenerator(@NotNull JsCheckiOCourseBuilder builder,
                                         @NotNull Course course) {
    super(builder, course);
  }

  @Override
  protected void createAdditionalFiles(@NotNull Project project, @NotNull VirtualFile baseDir) {

  }

  @Override
  protected boolean beforeProjectGenerated() {
    try {
      final CheckiOCourseContentGenerator contentGenerator =
        new CheckiOCourseContentGenerator(JavaScriptFileType.INSTANCE, JsCheckiOApiConnector.getInstance());

      final List<CheckiOStation> stations = contentGenerator.getStationsFromServerUnderProgress();

      stations.forEach(((CheckiOCourse) myCourse)::addStation);
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
