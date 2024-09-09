package com.jetbrains.edu.learning.marketplace.lti

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project

/**
 * Provides the access to LTI settings.
 * The settings are stored in the "lti.yaml" file in the course root.
 * YAML format is:
 * launches:
 *   - id: id-of-the-first-launch
 *     lms_description: Moodle 1 at some University
 *   - id: id-of-the-second-launch
 *     lms_description: Moodle 2 at another University
 */
@Service(Service.Level.PROJECT)
@State(name="LTISettings", reloadable = true, storages = [Storage("lti.xml")])
class LTISettingsManager : SimplePersistentStateComponent<LTISettings>(LTISettings()) {
  companion object {
    fun instance(project: Project) = project.service<LTISettingsManager>()
  }
}