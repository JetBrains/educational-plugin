package com.jetbrains.edu.python.learning.checkio.checker;

import com.jetbrains.edu.learning.checkio.CheckiOCourseContentGenerator;
import com.jetbrains.edu.learning.checkio.checker.CheckiOCheckListener;
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOCourse;
import com.jetbrains.edu.learning.courseFormat.ext.CourseExt;
import com.jetbrains.edu.python.learning.checkio.connectors.PyCheckiOApiConnector;
import com.jetbrains.edu.python.learning.checkio.connectors.PyCheckiOOAuthConnector;
import com.jetbrains.python.PythonFileType;
import com.jetbrains.python.PythonLanguage;
import org.jetbrains.annotations.NotNull;

public class PyCheckiOCheckListener extends CheckiOCheckListener {
  public PyCheckiOCheckListener() {
    super(
      new CheckiOCourseContentGenerator(
        PythonFileType.INSTANCE,
        PyCheckiOApiConnector.INSTANCE
      ),
      PyCheckiOOAuthConnector.INSTANCE
    );
  }

  @Override
  protected boolean isEnabledForCourse(@NotNull CheckiOCourse course) {
    return PythonLanguage.INSTANCE == CourseExt.getLanguageById(course);
  }
}
