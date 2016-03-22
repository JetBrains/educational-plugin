package com.jetbrains.edu.kotlin;

import com.intellij.openapi.project.Project;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.StudyTwitterPluginConfigurator;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.StudyStatus;
import com.jetbrains.edu.learning.courseFormat.Task;
import com.jetbrains.edu.learning.twitter.StudyTwitterUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EduKotlinTwitterConfigurator implements StudyTwitterPluginConfigurator {


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
                    && solvedTask.getStatus() == StudyStatus.Solved
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

    @Override
    public void setAskToTweet(@NotNull final Project project, boolean askToTweet) {
        KotlinStudyTwitterSettings.getInstance(project).setAskToTweet(askToTweet);
    }

    @Override
    public boolean accept(@NotNull Project project) {
        StudyTaskManager instance = StudyTaskManager.getInstance(project);
        if (instance == null) return false;
        Course course = instance.getCourse();
        return course != null && "PyCharm".equals(course.getCourseType()) && "kotlin".equals(course.getLanguage());
    }
}
