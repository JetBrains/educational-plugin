package com.jetbrains.edu.kotlin;

import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.intellij.generation.EduCourseModuleBuilder;
import com.jetbrains.edu.learning.intellij.generation.EduIntellijCourseProjectGeneratorBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.KotlinIcons;

import javax.swing.*;

class EduKotlinCourseProjectGenerator extends EduIntellijCourseProjectGeneratorBase {

  public EduKotlinCourseProjectGenerator(@NotNull Course course) {
    super(course);
  }

  @Nullable
  @Override
  protected EduCourseModuleBuilder studyModuleBuilder() {
    return new EduKotlinKoansModuleBuilder(myCourse);
  }

  @Nullable
  @Override
  public Icon getLogo() {
    return KotlinIcons.SMALL_LOGO;
  }
}
