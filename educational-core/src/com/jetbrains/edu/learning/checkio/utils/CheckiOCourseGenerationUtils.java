package com.jetbrains.edu.learning.checkio.utils;

import com.intellij.ide.BrowserUtil;
import com.jetbrains.edu.learning.checkio.CheckiOCourseContentGenerator;
import com.jetbrains.edu.learning.checkio.account.CheckiOAccount;
import com.jetbrains.edu.learning.checkio.api.exceptions.HttpException;
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOCourse;
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOStation;
import com.jetbrains.edu.learning.configuration.CourseCantBeStartedException;
import com.jetbrains.edu.learning.newproject.ui.ErrorState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class CheckiOCourseGenerationUtils {

  public static void getCourseFromServerUnderProgress(@NotNull CheckiOCourseContentGenerator contentGenerator,
                                                      @NotNull CheckiOCourse course,
                                                      @Nullable CheckiOAccount account,
                                                      String link) throws CourseCantBeStartedException {
    try {
      final List<CheckiOStation> stations = contentGenerator.getStationsFromServerUnderProgress();
      stations.forEach(course::addStation);
    }
    catch (Exception e) {
      throw new CourseCantBeStartedException(getErrorState(e, account, link));
    }
  }

  private static ErrorState getErrorState(@NotNull Exception e, @Nullable CheckiOAccount account, String link) {
    if (e instanceof HttpException &&
        ((HttpException)e).getResponse().code() == 401 && account != null) {
      return new ErrorState.CustomSevereError("", "Open", " CheckiO to verify account and try again",
                                              () -> BrowserUtil.browse(link + "/login/checkio/"));
    }
    return new ErrorState.CustomSevereError(e.getMessage(), "", "", null);
  }
}
