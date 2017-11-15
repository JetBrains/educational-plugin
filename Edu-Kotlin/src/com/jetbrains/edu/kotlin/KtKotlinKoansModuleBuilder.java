package com.jetbrains.edu.kotlin;

import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.ProjectWizardStepFactory;
import com.intellij.ide.util.projectWizard.SettingsStep;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.ModifiableModuleModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleWithNameAlreadyExists;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Conditions;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.io.FileUtil;
import com.jetbrains.edu.learning.StudyUtils;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.intellij.generation.EduCourseModuleBuilder;
import com.jetbrains.edu.learning.intellij.generation.EduModuleBuilderUtils;
import com.jetbrains.edu.learning.intellij.generation.EduProjectGenerator;
import org.jdom.JDOMException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;

class KtKotlinKoansModuleBuilder extends EduCourseModuleBuilder {
  static final String DEFAULT_COURSE_NAME = "Kotlin Koans.zip";
  private static final Logger LOG = Logger.getInstance(KtKotlinKoansModuleBuilder.class);
  @Nullable private Course myCourse;

  KtKotlinKoansModuleBuilder(@Nullable Course course) {
    this.myCourse = course;
  }

  @Override
  public String getBuilderId() {
    return "kotlin.edu.builder";
  }

  @Nullable
  @Override
  public ModuleWizardStep modifySettingsStep(@NotNull SettingsStep settingsStep) {
    return ProjectWizardStepFactory.getInstance().createJavaSettingsStep(settingsStep, this, Conditions.alwaysTrue());
  }

  @NotNull
  @Override
  public Module createModule(@NotNull ModifiableModuleModel moduleModel) throws InvalidDataException, IOException, ModuleWithNameAlreadyExists, JDOMException, ConfigurationException {
    Module baseModule = super.createModule(moduleModel);
    Project project = baseModule.getProject();
    EduProjectGenerator generator = new EduProjectGenerator();
    if (myCourse == null) {
      File courseRoot = StudyUtils.getBundledCourseRoot(DEFAULT_COURSE_NAME, KtKotlinKoansModuleBuilder.class);
      final Course course = generator.addLocalCourse(FileUtil.join(courseRoot.getPath(), DEFAULT_COURSE_NAME));
      if (course == null) {
        LOG.info("Failed to find course " + DEFAULT_COURSE_NAME);
        return baseModule;
      }
    }
    myCourse.setLanguage("kotlin");
    EduModuleBuilderUtils.createCourseFromCourseInfo(moduleModel, project, generator, myCourse, getModuleFileDirectory());
    return baseModule;
  }

  @Nullable
  @Override
  protected Course getCourse() {
    return myCourse;
  }
}