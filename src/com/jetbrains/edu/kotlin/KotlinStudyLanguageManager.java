package com.jetbrains.edu.kotlin;

import com.jetbrains.edu.learning.StudyLanguageManager;
import org.jetbrains.annotations.NotNull;


public class KotlinStudyLanguageManager implements StudyLanguageManager {
    @NotNull
    @Override
    public String getTestFileName() {
        return "tests.kt";
    }

    @NotNull
    @Override
    public String getTestHelperFileName() {
        return KotlinStudyUtils.TEST_HELPER;
    }

    @NotNull
    @Override
    public String getUserTester() {
        return "userTester.kt";
    }
}
