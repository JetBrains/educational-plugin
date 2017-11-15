package com.jetbrains.edu.kotlin;

import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.intellij.generation.EduCourseModuleBuilder;
import com.jetbrains.edu.learning.intellij.generation.EduIntellijCourseProjectGeneratorBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class KtCourseProjectGenerator extends EduIntellijCourseProjectGeneratorBase {

  public KtCourseProjectGenerator(@NotNull Course course) {
    super(course);
  }

  @Nullable
  @Override
  protected EduCourseModuleBuilder studyModuleBuilder() {
    return new KtKotlinKoansModuleBuilder(myCourse);
  }
}
