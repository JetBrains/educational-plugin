package com.jetbrains.edu.kotlin;

import com.intellij.openapi.project.Project;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.StudyTwitterPluginConfigurator;
import com.jetbrains.edu.learning.core.EduNames;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.StudyStatus;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.twitter.StudyTwitterUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class KtTwitterConfigurator implements StudyTwitterPluginConfigurator {


    @NotNull
    @Override
    public String getConsumerKey(@NotNull Project project) {
        return KtTwitterBundle.message("consumerKey");
    }

    @NotNull
    @Override
    public String getConsumerSecret(@NotNull Project project) {
        return KtTwitterBundle.message("consumerSecret");
    }


    @Override
    public void storeTwitterTokens(@NotNull Project project, @NotNull String accessToken, @NotNull String tokenSecret) {
        KtTwitterSettings ktTwitterSettings = KtTwitterSettings.getInstance(project);
        ktTwitterSettings.setAccessToken(accessToken);
        ktTwitterSettings.setTokenSecret(tokenSecret);
    }

    @NotNull
    @Override
    public String getTwitterTokenSecret(@NotNull Project project) {
        KtTwitterSettings ktTwitterSettings = KtTwitterSettings.getInstance(project);
        return ktTwitterSettings.getTokenSecret();
    }

    @NotNull
    @Override
    public String getTwitterAccessToken(@NotNull Project project) {
        KtTwitterSettings ktTwitterSettings = KtTwitterSettings.getInstance(project);
        return ktTwitterSettings.getAccessToken();
    }

    @Override
    public boolean askToTweet(@NotNull Project project, Task solvedTask, StudyStatus statusBeforeCheck) {
        StudyTaskManager taskManager = StudyTaskManager.getInstance(project);
        Course course = taskManager.getCourse();
        if (course != null && course.getName().equals("Kotlin Koans")) {
            KtTwitterSettings ktTwitterSettings = KtTwitterSettings.getInstance(project);
            return ktTwitterSettings.askToTweet()
                    && solvedTask.getStatus() == StudyStatus.Solved
                    && (statusBeforeCheck == StudyStatus.Unchecked || statusBeforeCheck == StudyStatus.Failed)
                    && KotlinUtils.calculateTaskNumber(solvedTask) % 8 == 0;
        }
        return false;
    }

    @Nullable
    @Override
    public StudyTwitterUtils.TwitterDialogPanel getTweetDialogPanel(@NotNull Task solvedTask) {
        return new KtTwitterDialogPanel(solvedTask);
    }

    @Override
    public void setAskToTweet(@NotNull final Project project, boolean askToTweet) {
        KtTwitterSettings.getInstance(project).setAskToTweet(askToTweet);
    }

    @Override
    public boolean accept(@NotNull Project project) {
        StudyTaskManager instance = StudyTaskManager.getInstance(project);
        if (instance == null) return false;
        Course course = instance.getCourse();
        return course != null && EduNames.PYCHARM.equals(course.getCourseType()) && "kotlin".equals(course.getLanguage());
    }
}
