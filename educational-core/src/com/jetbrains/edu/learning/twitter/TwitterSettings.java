package com.jetbrains.edu.learning.twitter;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@State(name = "KotlinStudyTwitterSettings", storages = @Storage("kotlin_study_twitter_settings.xml"))
public class TwitterSettings implements PersistentStateComponent<TwitterSettings.State> {

    private State myState = new State();


    public static class State {
        public boolean askToTweet = true;
        public String accessToken = "";
        public String tokenSecret = "";
    }

    public static TwitterSettings getInstance(@NotNull final Project project) {
        return ServiceManager.getService(project, TwitterSettings.class);
    }
    @Nullable
    @Override
    public State getState() {
        return myState;
    }

    @Override
    public void loadState(@NotNull State state) {
        myState = state;
    }

    public boolean askToTweet() {
        return myState.askToTweet;
    }

    public void setAskToTweet(final boolean askToTweet) {
        myState.askToTweet = askToTweet;
    }

    @NotNull
    public String getAccessToken() {
        return myState.accessToken;
    }

    public void setAccessToken(@NotNull String accessToken) {
        myState.accessToken = accessToken;
    }

    @NotNull
    public String getTokenSecret() {
        return myState.tokenSecret;
    }

    public void setTokenSecret(@NotNull String tokenSecret) {
        myState.tokenSecret = tokenSecret;
    }
}
