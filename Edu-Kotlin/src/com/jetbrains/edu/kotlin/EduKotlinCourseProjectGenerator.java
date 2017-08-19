package com.jetbrains.edu.kotlin;

import com.intellij.facet.ui.ValidationResult;
import com.intellij.ide.util.newProjectWizard.AbstractProjectWizard;
import com.intellij.ide.util.newProjectWizard.StepSequence;
import com.intellij.ide.util.projectWizard.ProjectBuilder;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ui.configuration.actions.NewModuleAction;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.platform.DirectoryProjectGenerator;
import com.jetbrains.edu.utils.generation.EduIntellijCourseProjectGeneratorBase;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.KotlinIcons;

import javax.swing.*;

class EduKotlinCourseProjectGenerator extends EduIntellijCourseProjectGeneratorBase {

  @NotNull
  @Override
  public DirectoryProjectGenerator getDirectoryProjectGenerator() {
    return new DirectoryProjectGenerator() {
      @Nls
      @NotNull
      @Override
      public String getName() {
        return "Kotlin Koans generator";
      }

      @Nullable
      @Override
      public Icon getLogo() {
        return KotlinIcons.SMALL_LOGO;
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
            return new EduKotlinKoansModuleBuilder(myCourse);
          }
        });
        setJdk(project);
        setCompilerOutput(project);

      }

      @NotNull
      @Override
      public ValidationResult validate(@NotNull String baseDirPath) {
        return ValidationResult.OK;
      }
    };

  }
}
