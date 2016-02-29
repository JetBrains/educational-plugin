package com.jetbrains.edu.course.creator;

import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.ProjectWizardStepFactory;
import com.intellij.ide.util.projectWizard.SettingsStep;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.module.ModifiableModuleModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleWithNameAlreadyExists;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;
import com.intellij.openapi.util.Conditions;
import com.intellij.openapi.util.InvalidDataException;
import com.jetbrains.edu.learning.courseGeneration.StudyProjectGenerator;
import com.jetbrains.edu.stepic.CourseInfo;
import com.jetbrains.edu.utils.generation.EduCourseModuleBuilder;
import com.jetbrains.edu.utils.generation.EduProjectGenerator;
import org.jdom.JDOMException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.IOException;

class EduCustomCourseModuleBuilder extends EduCourseModuleBuilder {
    private EduProjectGenerator myGenerator = new EduProjectGenerator();
    //TODO: remove this after API change!!!
    private CourseInfo mySelectedCourse;

    @Nullable
    @Override
    public String getBuilderId() {
        return "custom.course.builder";
    }

    @Override
    public ModuleWizardStep[] createWizardSteps(@NotNull WizardContext wizardContext, @NotNull ModulesProvider modulesProvider) {
        return new ModuleWizardStep[] {new EduCourseSelectionSettingsStep(myGenerator)};
    }


    private class EduCourseSelectionSettingsStep extends ModuleWizardStep {

        private final StudyProjectGenerator myGenerator;

        EduCourseSelectionSettingsStep(StudyProjectGenerator generator) {
            myGenerator = generator;
        }

        @Override
        public JComponent getComponent() {
              return new EduLocalCoursePanel(myGenerator, EduCustomCourseModuleBuilder.this).getContentPanel();
        }

        @Override
        public void updateDataModel() {
        }
    }

    @Nullable
    @Override
    public ModuleWizardStep modifySettingsStep(@NotNull SettingsStep settingsStep) {
        return ProjectWizardStepFactory.getInstance().createJavaSettingsStep(settingsStep, this, Conditions.alwaysTrue());
    }

    void setSelectedCourse(CourseInfo selectedCourse) {
        mySelectedCourse = selectedCourse;
    }

    @NotNull
    @Override
    public Module createModule(@NotNull ModifiableModuleModel moduleModel) throws InvalidDataException, IOException, ModuleWithNameAlreadyExists, JDOMException, ConfigurationException {
        Module baseModule = super.createModule(moduleModel);
        if (mySelectedCourse != null) {
            createCourseFromCourseInfo(moduleModel, baseModule.getProject(), myGenerator, mySelectedCourse);
        }
        return baseModule;
    }
}
