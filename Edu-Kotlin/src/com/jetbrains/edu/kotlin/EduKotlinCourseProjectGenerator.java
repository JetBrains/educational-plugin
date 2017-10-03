package com.jetbrains.edu.kotlin;

import com.jetbrains.edu.learning.intellij.generation.EduCourseModuleBuilder;
import com.jetbrains.edu.learning.intellij.generation.EduIntellijCourseProjectGeneratorBase;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.KotlinIcons;

import javax.swing.*;

class EduKotlinCourseProjectGenerator extends EduIntellijCourseProjectGeneratorBase {

  @Override
  protected EduCourseModuleBuilder studyModuleBuilder() {
    return new EduKotlinKoansModuleBuilder(myCourse);
  }

  @Nullable
  @Override
  protected Icon getLogo() {
    return KotlinIcons.SMALL_LOGO;
  }
}
