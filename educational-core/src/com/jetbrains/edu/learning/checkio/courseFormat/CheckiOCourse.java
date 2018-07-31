package com.jetbrains.edu.learning.checkio.courseFormat;

import com.jetbrains.edu.learning.EduNames;
import com.jetbrains.edu.learning.courseFormat.Course;

public class CheckiOCourse extends Course {
  public CheckiOCourse() {
    setName("CheckiO");
    setDescription("CheckiO description");
    setLanguage(EduNames.CHECKIO_PYTHON);
  }
}
