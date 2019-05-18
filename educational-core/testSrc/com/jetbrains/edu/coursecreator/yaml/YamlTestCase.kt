package com.jetbrains.edu.coursecreator.yaml

import com.intellij.openapi.application.Experiments
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.jetbrains.edu.coursecreator.yaml.YamlFormatSettings.YAML_TEST_PROJECT_READY
import com.jetbrains.edu.learning.EduExperimentalFeatures
import com.jetbrains.edu.learning.EduTestCase

// TODO: reuse this class in test classes after merge
abstract class YamlTestCase : EduTestCase() {
  override fun setUp() {
    super.setUp()

    // In this method course is set before course files are created so `CCProjectComponent.createYamlConfigFilesIfMissing` is called
    // for course with no files. This flag is checked in this method and it does nothing if the flag is false
    project.putUserData(YAML_TEST_PROJECT_READY, true)
    Experiments.setFeatureEnabled(EduExperimentalFeatures.YAML_FORMAT, true)

    // we need to specify the file type for yaml files as otherwise they are recognised as binaries and aren't allowed to be edited
    // we don't add dependency on yaml plugin because it's impossible to add for tests only and we don't want to have redundant dependency
    // in production code
    runWriteAction { FileTypeManager.getInstance().associateExtension(PlainTextFileType.INSTANCE, "yaml") }
  }
}