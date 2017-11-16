package com.jetbrains.edu.java;

import com.intellij.openapi.module.ModifiableModuleModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleWithNameAlreadyExists;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.InvalidDataException;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.intellij.generation.CourseModuleBuilder;
import com.jetbrains.edu.learning.intellij.generation.IntellijCourseProjectGeneratorBase;
import com.jetbrains.edu.learning.intellij.generation.EduModuleBuilderUtils;
import com.jetbrains.edu.learning.intellij.generation.EduProjectGenerator;
import org.jdom.JDOMException;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class JCourseProjectGenerator extends IntellijCourseProjectGeneratorBase {

  public JCourseProjectGenerator(@NotNull Course course) {
    super(course);
  }

  @NotNull
  @Override
  protected CourseModuleBuilder studyModuleBuilder() {
    return new CourseModuleBuilder() {
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
}
