package com.jetbrains.edu.kotlin;

import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.ProjectWizardStepFactory;
import com.intellij.ide.util.projectWizard.SettingsStep;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationsConfiguration;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.ModifiableModuleModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleWithNameAlreadyExists;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.util.Conditions;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.io.FileUtil;
import com.jetbrains.edu.stepic.CourseInfo;
import com.jetbrains.edu.utils.EduIntellijUtils;
import com.jetbrains.edu.utils.generation.EduCourseModuleBuilder;
import com.jetbrains.edu.utils.generation.EduProjectGenerator;
import org.jdom.JDOMException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.configuration.ConfigureKotlinInProjectUtilsKt;
import org.jetbrains.kotlin.idea.configuration.KotlinProjectConfigurator;
import org.jetbrains.kotlin.idea.configuration.KotlinWithLibraryConfigurator;
import org.jetbrains.kotlin.idea.framework.ui.FileUIUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;


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
        configureKotlin(project);
        return baseModule;
    }

    private void configureKotlin(@NotNull final Project project) {
        StartupManager.getInstance(project).runWhenProjectIsInitialized(new Runnable() {
            @Override
            public void run() {
                //TODO: find more appropriate way to do this

                NotificationsConfiguration.getNotificationsConfiguration().changeSettings("Configure Kotlin: info notification",
                        NotificationDisplayType.NONE, true, false);
                KotlinProjectConfigurator configuratorByName = ConfigureKotlinInProjectUtilsKt.getConfiguratorByName("java");
                if (configuratorByName == null) {
                    LOG.info("Failed to find configurator");
                    return;
                }
                Class<?> confClass = configuratorByName.getClass();
                while (confClass != KotlinWithLibraryConfigurator.class) {
                    confClass = confClass.getSuperclass();
                }
                try {
                    String lib = FileUIUtils.createRelativePath(project, project.getBaseDir(), "lib");

                    Method method = confClass.getDeclaredMethod("configureModuleWithLibrary", Module.class, String.class, String.class);
                    method.setAccessible(true);
                    for (Module module : ModuleManager.getInstance(project).getModules()) {
                        method.invoke(configuratorByName, module, lib, null);
                    }
                } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                    LOG.info(e);
                    configuratorByName.configure(project, Collections.emptyList());
                }
                finally {
                    NotificationsConfiguration.getNotificationsConfiguration().changeSettings("Configure Kotlin: info notification",
                            NotificationDisplayType.STICKY_BALLOON, true, false);
                }
            }
        });
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

        createCourseFromCourseInfo(moduleModel, project, generator, courseInfo);
        return baseModule;
    }

}