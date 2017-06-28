package com.jetbrains.edu.java;

import com.intellij.facet.ui.ValidationResult;
import com.intellij.ide.util.newProjectWizard.AbstractProjectWizard;
import com.intellij.ide.util.newProjectWizard.StepSequence;
import com.intellij.ide.util.projectWizard.ProjectBuilder;
import com.intellij.openapi.module.ModifiableModuleModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleWithNameAlreadyExists;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ui.configuration.actions.NewModuleAction;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.platform.DirectoryProjectGenerator;
import com.jetbrains.edu.learning.intellij.generation.EduCourseModuleBuilder;
import com.jetbrains.edu.learning.intellij.generation.EduProjectGenerator;
import com.jetbrains.edu.utils.generation.EduIntellijCourseProjectGeneratorBase;
import com.jetbrains.edu.utils.generation.EduModuleBuilderUtils;
import org.jdom.JDOMException;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.IOException;


public class EduJavaCourseProjectGenerator extends EduIntellijCourseProjectGeneratorBase {

  @NotNull
  @Override
  public DirectoryProjectGenerator getDirectoryProjectGenerator() {
    return new DirectoryProjectGenerator() {

      @Nls
      @NotNull
      @Override
      public String getName() {
        return "Java Project Generator";
      }

      @Nullable
      @Override
      public Icon getLogo() {
        return null;
      }

      @Override
      public void generateProject(@NotNull Project project, @NotNull VirtualFile virtualFile, @NotNull Object o, @NotNull Module module) {
        configureProject(project);
        createCourseStructure(project);
      }

      private void configureProject(@NotNull Project project) {
        setJdk(project);
        setCompilerOutput(project);
      }

      private void createCourseStructure(@NotNull Project project) {
        new NewModuleAction().createModuleFromWizard(project, null, createProjectWizard(project));
      }

      @NotNull
      private AbstractProjectWizard createProjectWizard(@NotNull Project project) {
        return new AbstractProjectWizard("", project, project.getBaseDir().getPath()) {
          @Override
          public StepSequence getSequence() {
            return null;
          }

          @Override
          public ProjectBuilder getProjectBuilder() {
            return new EduCourseModuleBuilder() {
              @NotNull
              @Override
              public Module createModule(@NotNull ModifiableModuleModel moduleModel) throws InvalidDataException, IOException, ModuleWithNameAlreadyExists, JDOMException, ConfigurationException {
                Module baseModule = super.createModule(moduleModel);
                Project project = baseModule.getProject();
                EduProjectGenerator generator = new EduProjectGenerator();
                generator.setSelectedCourse(myCourse);
                EduModuleBuilderUtils.createCourseFromCourseInfo(moduleModel, project, generator, myCourse, getModuleFileDirectory());
                return baseModule;
              }
            };
          }
        };
      }

      @NotNull
      @Override
      public ValidationResult validate(@NotNull String s) {
        return ValidationResult.OK;
      }
    };

  }
}