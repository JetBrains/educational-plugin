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
import com.jetbrains.edu.learning.stepic.CourseInfo;
import com.jetbrains.edu.utils.EduIntellijUtils;
import com.jetbrains.edu.learning.intellij.generation.EduCourseModuleBuilder;
import com.jetbrains.edu.learning.intellij.generation.EduProjectGenerator;
import com.jetbrains.edu.utils.generation.EduModuleBuilderUtils;
import org.jdom.JDOMException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;


class EduKotlinKoansModuleBuilder extends EduCourseModuleBuilder {
    private static final String DEFAULT_COURSE_NAME = "Kotlin Koans.zip";
    private static final Logger LOG = Logger.getInstance(EduKotlinKoansModuleBuilder.class);

    @Override
    public String getBuilderId() {
        return "kotlin.edu.builder";
    }

    @Nullable
    @Override
    public Module commitModule(@NotNull Project project, @Nullable ModifiableModuleModel model) {
        Module baseModule = super.commitModule(project, model);
        new EduKotlinCourseConfigurator().configureModule(project);
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
        File courseRoot = EduIntellijUtils.getBundledCourseRoot(DEFAULT_COURSE_NAME, EduKotlinKoansModuleBuilder.class);
        CourseInfo courseInfo = generator.addLocalCourse(FileUtil.join(courseRoot.getPath(), DEFAULT_COURSE_NAME));
        if (courseInfo == null) {
            LOG.info("Failed to find course " + DEFAULT_COURSE_NAME);
            return baseModule;
        }

        EduModuleBuilderUtils.createCourseFromCourseInfo(moduleModel, project, generator, courseInfo, getModuleFileDirectory());
        return baseModule;
    }

}