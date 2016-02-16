package com.jetbrains.edu.kotlin;

import com.intellij.ide.util.projectWizard.JavaModuleBuilder;
import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.ProjectWizardStepFactory;
import com.intellij.ide.util.projectWizard.SettingsStep;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.ModifiableModuleModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleWithNameAlreadyExists;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.DumbModePermission;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtil;
import com.jetbrains.edu.courseFormat.Course;
import com.jetbrains.edu.courseFormat.Lesson;
import com.jetbrains.edu.learning.StudyProjectComponent;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.stepic.CourseInfo;
import com.jetbrains.edu.utils.EduIntellijUtils;
import org.jdom.JDOMException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.configuration.ConfigureKotlinInProjectUtilsKt;
import org.jetbrains.kotlin.idea.configuration.KotlinProjectConfigurator;
import org.jetbrains.kotlin.idea.configuration.KotlinWithLibraryConfigurator;
import org.jetbrains.kotlin.idea.framework.ui.FileUIUtils;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;


public class KotlinStudyModuleBuilder extends JavaModuleBuilder {
    private static final String DEFAULT_COURSE_NAME = "Kotlin Koans.zip";
    private static final Logger LOG = Logger.getInstance(KotlinStudyModuleBuilder.class);

    @Override
    public String getBuilderId() {
        return "kotlin.edu.builder";
    }

    @Nullable
    @Override
    public Module commitModule(@NotNull Project project, @Nullable ModifiableModuleModel model) {
        Module baseModule = super.commitModule(project, model);
        StartupManager.getInstance(project).runWhenProjectIsInitialized(new Runnable() {
            @Override
            public void run() {
                //TODO: find more appropriate way to do this
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
            }
        });
        return baseModule;
    }

    @Nullable
    @Override
    public ModuleWizardStep modifySettingsStep(@NotNull SettingsStep settingsStep) {
        return ProjectWizardStepFactory.getInstance().createJavaSettingsStep(settingsStep, this, Condition.TRUE);
    }

    @NotNull
    @Override
    public Module createModule(@NotNull ModifiableModuleModel moduleModel) throws InvalidDataException, IOException, ModuleWithNameAlreadyExists, JDOMException, ConfigurationException {
        Module baseModule = super.createModule(moduleModel);
        Project project = baseModule.getProject();
        EduKotlinStudyProjectGenerator generator = new EduKotlinStudyProjectGenerator();
        CourseInfo courseInfo = generator.addLocalCourse(FileUtil.toSystemDependentName(
                EduIntellijUtils.getBundledCourseRoot(DEFAULT_COURSE_NAME, KotlinStudyModuleBuilder.class).getAbsolutePath() + "/" + DEFAULT_COURSE_NAME));
        if (courseInfo != null) {
            generator.setSelectedCourse(courseInfo);
        }
        generator.generateProject(project, project.getBaseDir());

        Course course = StudyTaskManager.getInstance(project).getCourse();
        if (course == null) {
            LOG.info("failed to generate course");
            return baseModule;
        }
        String moduleDir = getModuleFileDirectory();
        EduUtilModuleBuilder utilModuleBuilder = new EduUtilModuleBuilder(moduleDir);
        Module utilModule = utilModuleBuilder.createModule(moduleModel);

        List<Lesson> lessons = course.getLessons();
        for (int i = 0; i < lessons.size(); i++) {
            int lessonVisibleIndex = i + 1;
            Lesson lesson = lessons.get(i);
            lesson.setIndex(lessonVisibleIndex);
            EduLessonModuleBuilder eduLessonModuleBuilder =  new EduLessonModuleBuilder(moduleDir, lesson, utilModule);
            eduLessonModuleBuilder.createModule(moduleModel);
        }
        //TODO: do smth with additional files etc

        ApplicationManager.getApplication().invokeLater(
                () -> DumbService.allowStartingDumbModeInside(DumbModePermission.MAY_START_BACKGROUND,
                        () -> ApplicationManager.getApplication().runWriteAction(() -> {
                            StudyProjectComponent.getInstance(project).registerStudyToolWindow(course);
                        })));
        return baseModule;
    }

    @Override
    public void setupRootModel(ModifiableRootModel rootModel) throws ConfigurationException {
        setSourcePaths(Collections.<Pair<String, String>>emptyList());
        super.setupRootModel(rootModel);
    }
}