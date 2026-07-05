package com.jetbrains.edu.go.environment;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Collection;

/**
 * Do not convert this class to Kotlin. The Go services used here are internal in Kotlin, so they will not be available.
 * This class is a temporary workaround until the proper API appears in the Go plugin.
 */
public final class GoDownloaderBridge {
  public static Collection<String> getAllAvailableGoSdkVersions(String os, String arch, @NotNull ProgressIndicator progressIndicator) throws IOException {
    @SuppressWarnings({"KotlinInternalInJava", "UnnecessaryFullyQualifiedName"})
    var goDownloadService = ApplicationManager.getApplication().getService(com.goide.sdk.download.GoDownloadSdkQueryService.class);
    var queryExecutor = goDownloadService.new QueryExecutor(null, progressIndicator);
    return queryExecutor.getVersionsForPlatform(os, arch, false);
  }
}
