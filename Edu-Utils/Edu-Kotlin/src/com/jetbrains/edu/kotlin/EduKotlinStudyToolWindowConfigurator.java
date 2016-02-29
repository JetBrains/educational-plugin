package com.jetbrains.edu.kotlin;

import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.Project;
import com.jetbrains.edu.courseFormat.Course;
import com.jetbrains.edu.learning.StudyBaseToolWindowConfigurator;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.actions.StudyFillPlaceholdersAction;
import com.jetbrains.edu.learning.actions.StudyNextStudyTaskAction;
import com.jetbrains.edu.learning.actions.StudyPreviousStudyTaskAction;
import com.jetbrains.edu.learning.actions.StudyRefreshTaskFileAction;
import org.jetbrains.annotations.NotNull;

public class EduKotlinStudyToolWindowConfigurator extends StudyBaseToolWindowConfigurator {
    @NotNull
    @Override
    public DefaultActionGroup getActionGroup(Project project) {
        final DefaultActionGroup group = new DefaultActionGroup();
        group.add(new EduKotlinCheckAction());
        group.add(new StudyPreviousStudyTaskAction());
        group.add(new StudyNextStudyTaskAction());
        group.add(new StudyRefreshTaskFileAction());
        StudyFillPlaceholdersAction fillPlaceholdersAction = new StudyFillPlaceholdersAction();
        fillPlaceholdersAction.getTemplatePresentation().setIcon(EduKotlinIcons.FILL_PLACEHOLDERS_ICON);
        fillPlaceholdersAction.getTemplatePresentation().setText("Fill Answer Placeholders");
        group.add(fillPlaceholdersAction);
        return group;


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
        return course != null && "PyCharm".equals(course.getCourseType()) && "kotlin".equals(course.getLanguage());
    }
}
