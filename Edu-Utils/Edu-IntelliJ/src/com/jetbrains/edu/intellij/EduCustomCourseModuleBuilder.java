package com.jetbrains.edu.intellij;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.SettingsStep;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;
import com.jetbrains.edu.EduNames;
import com.jetbrains.edu.learning.courseGeneration.StudyProjectGenerator;
import com.jetbrains.edu.stepic.CourseInfo;
import com.jetbrains.edu.utils.EduFromCourseModuleBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.framework.KotlinModuleSettingStep;
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform;

import javax.swing.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

class EduCustomCourseModuleBuilder extends EduFromCourseModuleBuilder {
    private StudyProjectGenerator myGenerator = new StudyProjectGenerator();
    //TODO: remove this after API change!!!
    private CourseInfo mySelectedCourse;

    @Nullable
    @Override
    public String getBuilderId() {
        return "custom.course.builder";
    }

    @Override
    protected StudyProjectGenerator getStudyProjectGenerator() {
        return myGenerator;
    }

    @Nullable
    @Override
    public ModuleWizardStep modifySettingsStep(@NotNull SettingsStep settingsStep) {
        if (mySelectedCourse != null) {
            //TODO: get language from course info
            File ourCoursesDir = new File(PathManager.getConfigPath(), "courses");
            File courseDir = new File(ourCoursesDir, mySelectedCourse.getName());
            if (courseDir.exists()) {
                String language = getLanguage(courseDir);
                if ("kotlin".equals(language)) {
                    return new KotlinModuleSettingStep(JvmPlatform.INSTANCE, this, settingsStep);
                }
            }
        }
        return super.modifyProjectTypeStep(settingsStep);
    }

    private static String getLanguage(File courseDir) {
        File courseFile = new File(courseDir, EduNames.COURSE_META_FILE);
        try {
            InputStreamReader reader = new InputStreamReader(new FileInputStream(courseFile), "UTF-8");
            JsonParser parser = new JsonParser();
            JsonObject obj = parser.parse(reader).getAsJsonObject();
            return obj.get("language").getAsString();
        } catch (Exception e) {
            return null;
        }
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

    void setSelectedCourse(CourseInfo selectedCourse) {
        mySelectedCourse = selectedCourse;
    }
}
