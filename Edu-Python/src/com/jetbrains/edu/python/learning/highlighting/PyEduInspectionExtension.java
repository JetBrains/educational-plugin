package com.jetbrains.edu.python.learning.highlighting;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.VirtualFileExt;
import com.jetbrains.edu.learning.courseFormat.EduFileErrorHighlightLevel;
import com.jetbrains.edu.learning.courseFormat.TaskFile;
import com.jetbrains.python.inspections.PyInspectionExtension;
import com.jetbrains.python.psi.PyElement;
import com.jetbrains.python.psi.PyImportStatementBase;
import com.jetbrains.python.psi.types.TypeEvalContext;
import org.jetbrains.annotations.NotNull;

public class PyEduInspectionExtension extends PyInspectionExtension {

  @Override
  public boolean ignoreUnresolvedReference(@NotNull PyElement element, @NotNull PsiReference reference, @NotNull TypeEvalContext context) {
    final PsiFile file = element.getContainingFile();
    final Project project = file.getProject();

    if (StudyTaskManager.getInstance(project).getCourse() == null) {
      return false;
    }
    TaskFile taskFile = VirtualFileExt.getTaskFile(file.getVirtualFile(), project);
    if (taskFile == null || taskFile.getErrorHighlightLevel() != EduFileErrorHighlightLevel.TEMPORARY_SUPPRESSION) {
      return false;
    }
    if (PsiTreeUtil.getParentOfType(element, PyImportStatementBase.class) != null) {
      return false;
    }
    return true;
  }

}
