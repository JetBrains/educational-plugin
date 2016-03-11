package com.jetbrains.edu.utils.generation;

import com.intellij.ide.util.projectWizard.JavaModuleBuilder;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.ModifiableModuleModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleWithNameAlreadyExists;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.DumbModePermission;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.util.InvalidDataException;
import com.jetbrains.edu.learning.StudyProjectComponent;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.Lesson;
import com.jetbrains.edu.learning.stepic.CourseInfo;
import org.jdom.JDOMException;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class EduCourseModuleBuilder extends JavaModuleBuilder {
    private static final Logger LOG = Logger.getInstance(EduCourseModuleBuilder.class);

    protected void createCourseFromCourseInfo(@NotNull ModifiableModuleModel moduleModel, Project project, EduProjectGenerator generator, CourseInfo courseInfo) throws InvalidDataException, IOException, ModuleWithNameAlreadyExists, JDOMException, ConfigurationException {
        generator.setSelectedCourse(courseInfo);
        generator.generateProject(project, project.getBaseDir());

        Course course = StudyTaskManager.getInstance(project).getCourse();
        if (course == null) {
            LOG.info("failed to generate course");
            return;
        }
        String moduleDir = getModuleFileDirectory();
        if (moduleDir == null) {
            return;
        }

        EduUtilModuleBuilder utilModuleBuilder = new EduUtilModuleBuilder(moduleDir);
        Module utilModule = utilModuleBuilder.createModule(moduleModel);

        createLessonModules(moduleModel, course, moduleDir, utilModule);

        ApplicationManager.getApplication().invokeLater(
                () -> DumbService.allowStartingDumbModeInside(DumbModePermission.MAY_START_BACKGROUND,
                        () -> ApplicationManager.getApplication().runWriteAction(() -> {
                            StudyProjectComponent.getInstance(project).registerStudyToolWindow(course);
                        })));
    }


    private void createLessonModules(@NotNull ModifiableModuleModel moduleModel, Course course, String moduleDir, Module utilModule) throws InvalidDataException, IOException, ModuleWithNameAlreadyExists, JDOMException, ConfigurationException {
        List<Lesson> lessons = course.getLessons();
        for (int i = 0; i < lessons.size(); i++) {
            int lessonVisibleIndex = i + 1;
            Lesson lesson = lessons.get(i);
            lesson.setIndex(lessonVisibleIndex);
            EduLessonModuleBuilder eduLessonModuleBuilder =  new EduLessonModuleBuilder(moduleDir, lesson, utilModule);
            eduLessonModuleBuilder.createModule(moduleModel);
        }
    }


    @Override
    public void setupRootModel(ModifiableRootModel rootModel) throws ConfigurationException {
        setSourcePaths(Collections.emptyList());
        super.setupRootModel(rootModel);
    }
}
