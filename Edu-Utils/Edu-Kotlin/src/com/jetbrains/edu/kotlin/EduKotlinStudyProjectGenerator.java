package com.jetbrains.edu.kotlin;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.edu.courseFormat.Course;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseGeneration.StudyProjectGenerator;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public class EduKotlinStudyProjectGenerator extends StudyProjectGenerator {
    private static final Logger LOG = Logger.getInstance(EduKotlinStudyProjectGenerator.class);

    @Override
    public void generateProject(@NotNull Project project, @NotNull VirtualFile baseDir) {
        final Course course = getCourse();
        if (course == null) {
            LOG.warn("Failed to get course");
            return;
        }
        //need this not to update course
        //when we update course we don't know anything about modules, so we create folders for lessons directly
        course.setUpToDate(true);
        StudyTaskManager.getInstance(project).setCourse(course);
        course.setCourseDirectory(new File(ourCoursesDir, mySelectedCourseInfo.getName()).getAbsolutePath());

    }
}
