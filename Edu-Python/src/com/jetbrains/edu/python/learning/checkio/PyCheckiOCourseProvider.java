package com.jetbrains.edu.python.learning.checkio;

import com.jetbrains.edu.learning.checkio.CheckiOCourseProvider;
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOCourse;
import com.jetbrains.edu.python.learning.checkio.utils.PyCheckiONames;
import org.jetbrains.annotations.NotNull;

public class PyCheckiOCourseProvider implements CheckiOCourseProvider {
  @NotNull
  public CheckiOCourse provideCourse() {
    return new CheckiOCourse(PyCheckiONames.PY_CHECKIO, PyCheckiONames.PY_CHECKIO_LANGUAGE);
  }
}
