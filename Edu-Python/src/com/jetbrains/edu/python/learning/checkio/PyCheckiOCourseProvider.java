package com.jetbrains.edu.python.learning.checkio;

import com.jetbrains.edu.learning.CoursesProvider;
import com.jetbrains.edu.learning.EduNames;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOCourse;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.python.learning.checkio.utils.PyCheckiONames;
import com.jetbrains.edu.python.learning.newproject.PyLanguageSettingsBase;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

import static com.jetbrains.edu.learning.EduNames.PYTHON_3_VERSION;

public class PyCheckiOCourseProvider implements CoursesProvider {
  @NotNull
  @Override
  public List<Course> loadCourses() {
    if (EduUtils.isAndroidStudio()) {
      return Collections.emptyList();
    }
    return Collections.singletonList(new CheckiOCourse(PyCheckiONames.PY_CHECKIO, EduNames.PYTHON + " " + PYTHON_3_VERSION));
  }
}
