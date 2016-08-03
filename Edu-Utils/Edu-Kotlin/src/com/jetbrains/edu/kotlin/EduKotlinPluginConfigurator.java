package com.jetbrains.edu.kotlin;

import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.jetbrains.edu.learning.StudyBasePluginConfigurator;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.StudyTwitterPluginConfigurator;
import com.jetbrains.edu.learning.StudyUtils;
import com.jetbrains.edu.learning.actions.*;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.twitter.StudyTwitterAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EduKotlinPluginConfigurator extends StudyBasePluginConfigurator {
    @NotNull
    @Override
    public DefaultActionGroup getActionGroup(Project project) {
        final DefaultActionGroup group = new DefaultActionGroup();
        EduKotlinCheckAction checkAction = new EduKotlinCheckAction();
        checkAction.getTemplatePresentation().setIcon(EduKotlinIcons.CHECK_TASK);
        group.add(checkAction);
        group.add(new StudyPreviousTaskAction());
        group.add(new StudyNextTaskAction());
        StudyRefreshTaskFileAction resetTaskFile = new StudyRefreshTaskFileAction();
        resetTaskFile.getTemplatePresentation().setIcon(EduKotlinIcons.RESET_TASK_FILE);
        group.add(resetTaskFile);
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
        return course != null && "PyCharm".equals(course.getCourseType()) && "kotlin".equals(course.getLanguageID());
    }

    @NotNull
    @Override
    public String getLanguageScriptUrl() {
        return getClass().getResource("/code_mirror/clike.js").toExternalForm();
    }

    @Nullable
    @Override
    public StudyAfterCheckAction[] getAfterCheckActions() {
        Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
        for (Project project : openProjects) {
            if (StudyTaskManager.getInstance(project).getCourse() != null) {
                StudyTwitterPluginConfigurator twitterConfigurator = StudyUtils.getTwitterConfigurator(project);
                if (twitterConfigurator != null) {
                    return new StudyAfterCheckAction[]{new StudyTwitterAction(twitterConfigurator)};
                }
            }
        }
        return null;
    }
}
