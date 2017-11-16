package com.jetbrains.edu.kotlin;

import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.intellij.generation.CourseModuleBuilder;
import com.jetbrains.edu.learning.intellij.generation.IntellijCourseProjectGeneratorBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class KtCourseProjectGenerator extends IntellijCourseProjectGeneratorBase {

  public KtCourseProjectGenerator(@NotNull Course course) {
    super(course);
  }

  @Nullable
  @Override
  protected CourseModuleBuilder studyModuleBuilder() {
    return new KtKotlinKoansModuleBuilder(myCourse);
  }
}
