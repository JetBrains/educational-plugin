package com.jetbrains.edu.python.learning.checkio.newProject;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.edu.learning.checkio.CheckiOCourseContentGenerator;
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOCourse;
import com.jetbrains.edu.learning.checkio.utils.CheckiOCourseGenerationUtils;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.python.learning.PyCourseBuilder;
import com.jetbrains.edu.python.learning.checkio.connectors.PyCheckiOApiConnector;
import com.jetbrains.edu.python.learning.newproject.PyCourseProjectGenerator;
import com.jetbrains.python.PythonFileType;
import org.jetbrains.annotations.NotNull;

public class PyCheckiOCourseProjectGenerator extends PyCourseProjectGenerator {
  private final CheckiOCourseContentGenerator myContentGenerator =
    new CheckiOCourseContentGenerator(PythonFileType.INSTANCE, PyCheckiOApiConnector.getInstance());

  public PyCheckiOCourseProjectGenerator(@NotNull PyCourseBuilder builder,
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
