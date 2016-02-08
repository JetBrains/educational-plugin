package com.jetbrains.edu.java;

import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.util.containers.HashMap;
import com.jetbrains.edu.courseFormat.Course;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.StudyToolWindowConfigurator;
import com.jetbrains.edu.learning.actions.StudyNextStudyTaskAction;
import com.jetbrains.edu.learning.actions.StudyPreviousStudyTaskAction;
import com.jetbrains.edu.learning.actions.StudyRefreshTaskFileAction;
import com.jetbrains.edu.learning.actions.StudyShowHintAction;
import com.jetbrains.edu.learning.ui.StudyToolWindow;
import com.jetbrains.edu.utils.EduIntellijUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class EduJavaStudyToolWindowConfigurator implements StudyToolWindowConfigurator {
    @NotNull
    @Override
    public DefaultActionGroup getActionGroup(Project project) {
        DefaultActionGroup group = new DefaultActionGroup();
        group.add(new EduJavaCheckAction());
        group.add(new StudyPreviousStudyTaskAction());
        group.add(new StudyNextStudyTaskAction());
        group.add(new StudyRefreshTaskFileAction());
        group.add(new StudyShowHintAction());
        return group;
    }

    @NotNull
    @Override
    public HashMap<String, JPanel> getAdditionalPanels(Project project) {
        return new HashMap<String, JPanel>();
    }

    @NotNull
    @Override
    public FileEditorManagerListener getFileEditorManagerListener(@NotNull Project project, @NotNull StudyToolWindow studyToolWindow) {
        return EduIntellijUtils.getFileEditorManagerListener(studyToolWindow, project);
    }

    @NotNull
    @Override
    public String getDefaultHighlightingMode() {
        return "text/x-java";
    }

    @Override
    public boolean accept(@NotNull Project project) {
        StudyTaskManager instance = StudyTaskManager.getInstance(project);
        if (instance == null) return false;
        Course course = instance.getCourse();
        return course != null && "PyCharm".equals(course.getCourseType()) && "JAVA".equals(course.getLanguage());
    }
}
