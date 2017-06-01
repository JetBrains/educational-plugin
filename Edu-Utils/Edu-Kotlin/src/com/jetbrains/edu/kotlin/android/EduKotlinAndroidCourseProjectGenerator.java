package com.jetbrains.edu.kotlin.android;

import com.intellij.facet.ui.ValidationResult;
import com.intellij.ide.util.newProjectWizard.AbstractProjectWizard;
import com.intellij.ide.util.newProjectWizard.StepSequence;
import com.intellij.ide.util.projectWizard.ProjectBuilder;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ui.configuration.actions.NewModuleAction;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.platform.DirectoryProjectGenerator;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.newproject.EduCourseProjectGenerator;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

class EduKotlinAndroidCourseProjectGenerator implements EduCourseProjectGenerator {
  private Course myCourse;
  @NotNull
  @Override
  public DirectoryProjectGenerator getDirectoryProjectGenerator() {
    return new DirectoryProjectGenerator() {
      @Nls
      @NotNull
      @Override
      public String getName() {
        return "Kotlin Android generator";
      }

      @Nullable
      @Override
      public Icon getLogo() {
        return null;
      }

      @Override
      public void generateProject(@NotNull Project project, @NotNull VirtualFile baseDir, @Nullable Object settings, @NotNull Module module) {
        new NewModuleAction().createModuleFromWizard(project, null, new AbstractProjectWizard("", project, baseDir.getPath()) {
          @Override
          public StepSequence getSequence() {
            return null;
          }

          @Override
          public ProjectBuilder getProjectBuilder() {
            return new EduKotlinAndroidModuleBuilder(myCourse);
          }
        });
      }

      @NotNull
      @Override
      public ValidationResult validate(@NotNull String baseDirPath) {
        return ValidationResult.OK;
      }
    };
  }

  @Nullable
  @Override
  public Object getProjectSettings() {
    return null;
  }

  @Override
  public void setCourse(@NotNull Course course) {
    myCourse = course;
  }

  @Override
  public ValidationResult validate() {
    return ValidationResult.OK;
  }

  @Override
  public boolean beforeProjectGenerated() {
    return true;
  }

  @Override
  public void afterProjectGenerated(@NotNull Project project) {

  }
}
