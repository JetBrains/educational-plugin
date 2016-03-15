package com.jetbrains.edu.kotlin;

import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.Project;
import com.jetbrains.edu.courseFormat.Course;
import com.jetbrains.edu.courseFormat.StudyStatus;
import com.jetbrains.edu.courseFormat.Task;
import com.jetbrains.edu.learning.StudyBasePluginConfigurator;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.actions.*;
import com.jetbrains.edu.learning.settings.ModifiableSettingsPanel;
import com.jetbrains.edu.learning.twitter.StudyTwitterAction;
import com.jetbrains.edu.learning.twitter.StudyTwitterUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EduKotlinPluginConfigurator extends StudyBasePluginConfigurator {
    private Project myProject;
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
        if (course != null && "PyCharm".equals(course.getCourseType()) && "kotlin".equals(course.getLanguage())) {
            myProject = project;
            return true;
        }
        return false;
    }

    @NotNull
    @Override
    public String getLanguageScriptUrl() {
        return getClass().getResource("/code_mirror/clike.js").toExternalForm();
    }

    @Nullable
    @Override
    public ModifiableSettingsPanel getSettingsPanel() {
        return new KotlinSettingsPanel(myProject);
    }

    @Nullable
    @Override
    public StudyAfterCheckAction[] getAfterCheckActions() {
        return new StudyAfterCheckAction[]{new StudyTwitterAction()};
    }

    @NotNull
    @Override
    public String getConsumerKey(@NotNull Project project) {
        return KotlinTwitterBundle.message("consumerKey");
    }

    @NotNull
    @Override
    public String getConsumerSecret(@NotNull Project project) {
        return KotlinTwitterBundle.message("consumerSecret");
    }
    

    @Override
    public void storeTwitterTokens(@NotNull Project project, @NotNull String accessToken, @NotNull String tokenSecret) {
        KotlinStudyTwitterSettings kotlinStudyTwitterSettings = KotlinStudyTwitterSettings.getInstance(project);
        kotlinStudyTwitterSettings.setAccessToken(accessToken);
        kotlinStudyTwitterSettings.setTokenSecret(tokenSecret);
    }

    @NotNull
    @Override
    public String getTwitterTokenSecret(@NotNull Project project) {
        KotlinStudyTwitterSettings kotlinStudyTwitterSettings = KotlinStudyTwitterSettings.getInstance(project);
        return kotlinStudyTwitterSettings.getTokenSecret();
    }

    @NotNull
    @Override
    public String getTwitterAccessToken(@NotNull Project project) {
        KotlinStudyTwitterSettings kotlinStudyTwitterSettings = KotlinStudyTwitterSettings.getInstance(project);
        return kotlinStudyTwitterSettings.getAccessToken();
    }

    @Override
    public boolean askToTweet(@NotNull Project project, Task solvedTask, StudyStatus statusBeforeCheck) {
        StudyTaskManager taskManager = StudyTaskManager.getInstance(project);
        Course course = taskManager.getCourse();
        if (course != null && course.getName().equals("Kotlin Koans")) {
            KotlinStudyTwitterSettings kotlinStudyTwitterSettings = KotlinStudyTwitterSettings.getInstance(project);
            return kotlinStudyTwitterSettings.askToTweet()
                    && (statusBeforeCheck == StudyStatus.Unchecked || statusBeforeCheck == StudyStatus.Failed)
                    && KotlinUtils.calculateTaskNumber(solvedTask) % 8 == 0;
        }
        return false;
    }

    @Nullable
    @Override
    public StudyTwitterUtils.TwitterDialogPanel getTweetDialogPanel(@NotNull Task solvedTask) {
        return new KotlinTwitterDialogPanel(solvedTask);
    }
}
