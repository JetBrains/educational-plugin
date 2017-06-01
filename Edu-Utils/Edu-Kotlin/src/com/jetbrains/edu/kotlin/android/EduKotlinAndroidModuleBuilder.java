package com.jetbrains.edu.kotlin.android;

import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.ProjectWizardStepFactory;
import com.intellij.ide.util.projectWizard.SettingsStep;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.ModifiableModuleModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleWithNameAlreadyExists;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.DumbModePermission;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Conditions;
import com.intellij.openapi.util.InvalidDataException;
import com.jetbrains.edu.learning.EduPluginConfigurator;
import com.jetbrains.edu.learning.StudyUtils;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.intellij.generation.EduCourseModuleBuilder;
import com.jetbrains.edu.learning.intellij.generation.EduProjectGenerator;
import org.jdom.JDOMException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

class EduKotlinAndroidModuleBuilder extends EduCourseModuleBuilder {
  private final Course myCourse;

  public EduKotlinAndroidModuleBuilder(Course myCourse) {
    this.myCourse = myCourse;
  }

  @Override
  public String getBuilderId() {
    return "kotlin.android.edu.builder";
  }

  @Nullable
  @Override
  public Module commitModule(@NotNull Project project, @Nullable ModifiableModuleModel model) {
    Module baseModule = super.commitModule(project, model);
    if (baseModule == null) {
      return null;
    }
    return baseModule;
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
    generator.setSelectedCourse(myCourse);
    generator.generateProject(project, project.getBaseDir());

    EduPluginConfigurator.INSTANCE.
        forLanguage(myCourse.getLanguageById()).createCourseModuleContent(moduleModel, project, myCourse, project.getBasePath());
    ApplicationManager.getApplication().invokeLater(() -> DumbService.allowStartingDumbModeInside(DumbModePermission.MAY_START_BACKGROUND,
        () -> ApplicationManager.getApplication().runWriteAction(() -> StudyUtils.registerStudyToolWindow(myCourse, project))));
    return baseModule;
  }

}