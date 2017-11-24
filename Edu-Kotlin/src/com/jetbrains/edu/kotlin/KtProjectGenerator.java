package com.jetbrains.edu.kotlin;

import com.intellij.ide.impl.NewProjectUtil;
import com.intellij.ide.util.newProjectWizard.AddModuleWizard;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.edu.coursecreator.CCUtils;
import com.jetbrains.edu.coursecreator.actions.CCCreateLesson;
import com.jetbrains.edu.coursecreator.actions.CCCreateTask;
import com.jetbrains.edu.learning.EduNames;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.Lesson;
import com.jetbrains.edu.learning.courseFormat.RemoteCourse;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.intellij.JdkProjectSettings;
import com.jetbrains.edu.learning.intellij.generation.CourseModuleBuilder;
import com.jetbrains.edu.learning.intellij.generation.EduGradleModuleGenerator;
import com.jetbrains.edu.learning.intellij.generation.IntellijCourseProjectGeneratorBase;
import com.jetbrains.edu.learning.stepik.StepikConnector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.service.project.wizard.GradleProjectImportBuilder;
import org.jetbrains.plugins.gradle.service.project.wizard.GradleProjectImportProvider;

import java.io.File;
import java.io.IOException;

public class KtProjectGenerator extends IntellijCourseProjectGeneratorBase {

    private static final Logger LOG = Logger.getInstance(KtProjectGenerator.class);

    public KtProjectGenerator(@NotNull Course course) {
        super(course);
    }

    @Override
    public void createCourseProject(@NotNull String location, @NotNull Object projectSettings) {
        File locationFile = new File(FileUtil.toSystemDependentName(location));
        if (!locationFile.exists() && !locationFile.mkdirs()) {
            return;
        }

        VirtualFile baseDir = WriteAction.compute(() -> LocalFileSystem.getInstance().refreshAndFindFileByIoFile(locationFile));
        if (baseDir == null) {
            LOG.error("Couldn't find '" + locationFile + "' in VFS");
            return;
        }
        VfsUtil.markDirtyAndRefresh(false, true, true, baseDir);

        ApplicationManager.getApplication().runWriteAction(() -> {
            try {
                EduGradleModuleGenerator.createProjectGradleFiles(location, locationFile.getName());
            } catch (IOException e) {
                LOG.error("Failed to generate project with gradle", e);
            }
        });

        final ProjectDataManager projectDataManager = ProjectDataManager.getInstance();
        GradleProjectImportBuilder gradleProjectImportBuilder = new GradleProjectImportBuilder(projectDataManager);
        final GradleProjectImportProvider gradleProjectImportProvider = new GradleProjectImportProvider(gradleProjectImportBuilder);
        AddModuleWizard wizard = new AddModuleWizard(null, baseDir.getPath(), gradleProjectImportProvider);
        Project project = NewProjectUtil.createFromWizard(wizard, null);

        ApplicationManager.getApplication().runWriteAction(() -> {
            try {
                StudyTaskManager.getInstance(project).setCourse(myCourse);
                if (CCUtils.isCourseCreator(project)) {
                    Lesson lesson = new CCCreateLesson().createAndInitItem(myCourse, null, EduNames.LESSON + 1, 1);
                    Task task = new CCCreateTask().createAndInitItem(myCourse, lesson, EduNames.TASK + 1, 1);
                    lesson.addTask(task);
                    myCourse.getLessons(true).add(lesson);
                    KtCourseBuilder.initTask(task);
                }
                Course course = myCourse;
                if (course instanceof RemoteCourse) {
                    course = StepikConnector.getCourse(project, (RemoteCourse) course);
                    if (course == null) {
                        LOG.error("Failed to get course from stepik");
                        return;
                    }
                }
                course.initCourse(false);
                EduGradleModuleGenerator.createCourseContent(course, location);

                setJdk(project, (JdkProjectSettings) projectSettings);
            } catch (IOException e) {
                LOG.error("Failed to generate course", e);
            }
        });

    }

    @Nullable
    @Override
    protected CourseModuleBuilder studyModuleBuilder() {
        return null;
    }
}
