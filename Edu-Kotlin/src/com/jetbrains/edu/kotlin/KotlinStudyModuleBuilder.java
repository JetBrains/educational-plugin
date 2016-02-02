package com.jetbrains.edu.kotlin;

import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.SettingsStep;
import com.intellij.openapi.module.ModifiableModuleModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.jetbrains.edu.utils.EduFromCourseModuleBuilder;
import com.jetbrains.edu.utils.EduIntellijUtils;
import com.jetbrains.edu.learning.courseGeneration.StudyProjectGenerator;
import com.jetbrains.edu.stepic.CourseInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.framework.KotlinModuleSettingStep;
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform;


public class KotlinStudyModuleBuilder extends EduFromCourseModuleBuilder {
    private  final StudyProjectGenerator myGenerator = new StudyProjectGenerator();
    private static final String DEFAULT_COURSE_NAME = "Kotlin Koans.zip";

    @Override
    public String getBuilderId() {
        return "kotlin.edu.builder";
    }

    public ModuleWizardStep modifySettingsStep(@NotNull SettingsStep settingsStep) {
        return new KotlinModuleSettingStep(JvmPlatform.INSTANCE, this, settingsStep);
    }

    @Override
    protected StudyProjectGenerator getStudyProjectGenerator() {
        return  myGenerator;
    }

    @Nullable
    @Override
    public Module commitModule(@NotNull Project project, @Nullable ModifiableModuleModel model) {
        CourseInfo courseInfo = myGenerator.addLocalCourse(FileUtil.toSystemDependentName(
                EduIntellijUtils.getBundledCourseRoot(DEFAULT_COURSE_NAME, KotlinStudyModuleBuilder.class).getAbsolutePath() + "/" + DEFAULT_COURSE_NAME));
        if (courseInfo != null) {
            myGenerator.setSelectedCourse(courseInfo);
        }
        return super.commitModule(project, model);
    }
}