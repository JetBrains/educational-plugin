package com.jetbrains.edu.javascript.learning.checkio.newProject;

import com.intellij.lang.javascript.JavaScriptFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.edu.javascript.learning.JsNewProjectSettings;
import com.jetbrains.edu.javascript.learning.checkio.JsCheckiOCourseBuilder;
import com.jetbrains.edu.javascript.learning.checkio.connectors.JsCheckiOApiConnector;
import com.jetbrains.edu.learning.checkio.CheckiOCourseContentGenerator;
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOCourse;
import com.jetbrains.edu.learning.checkio.utils.CheckiOCourseGenerationUtils;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator;
import org.jetbrains.annotations.NotNull;

public class JsCheckiOCourseProjectGenerator extends CourseProjectGenerator<JsNewProjectSettings> {
  private final CheckiOCourseContentGenerator myContentGenerator =
    new CheckiOCourseContentGenerator(JavaScriptFileType.INSTANCE, JsCheckiOApiConnector.getInstance());

  public JsCheckiOCourseProjectGenerator(@NotNull JsCheckiOCourseBuilder builder,
                                         @NotNull Course course) {
    super(builder, course);
  }

  @Override
  protected void createAdditionalFiles(@NotNull Project project, @NotNull VirtualFile baseDir) {

  }

  @Override
  protected boolean beforeProjectGenerated() {
    return CheckiOCourseGenerationUtils.generateCourseFromServerUnderProgress(myContentGenerator, (CheckiOCourse) myCourse);
  }
}
