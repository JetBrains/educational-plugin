/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jetbrains.edu.learning;

import org.jetbrains.annotations.NonNls;

@NonNls
public class EduNames {

  public static final String PLUGIN_ID = "com.jetbrains.edu";

  public static final String JBA = "JetBrains Academy";

  public static final String EDU_PREFIX = "edu";
  public static final String TASK_HTML = "task.html";
  public static final String TASK_MD = "task.md";
  public static final String HINTS = "hints";
  public static final String LESSON = "lesson";
  public static final String FRAMEWORK_LESSON = "framework lesson";
  public static final String FRAMEWORK = "framework";
  public static final String SECTION = "section";
  public static final String TASK = "task";
  public static final String COURSE = "course";
  public static final String ITEM = "item";
  public static final String PROJECT = "project";
  public static final String TEST_TAB_NAME = "test";
  public static final String USER_TEST_INPUT = "input";
  public static final String USER_TEST_OUTPUT = "output";
  public static final String WINDOW_POSTFIX = "_window.";
  public static final String WINDOWS_POSTFIX = "_windows";
  public static final String ANSWERS_POSTFIX = "_answers";
  public static final String USER_TESTS = "userTests";
  public static final String TEST_HELPER = "test_helper.py";

  public static final String COURSE_META_FILE = "course.json";
  public static final String ADDITIONAL_MATERIALS = "Edu additional materials";
  public static final String PROJECT_NAME = "PROJECT_NAME";
  public static final String RUN_CONFIGURATION_DIR = "runConfigurations";

  public static final String DEFAULT_ENVIRONMENT = "";

  // Used as course type only
  public static final String PYCHARM = "PyCharm";
  public static final String EDU = "Edu";
  public static final String ANDROID = "Android";
  public static final String UNITTEST = "unittest";

  public static final String SRC = "src";
  public static final String TEST = "test";
  public static final String BUILD = "build";
  public static final String OUT = "out";

  public static final String STUDY_PROJECT_XML_PATH = "/.idea/study_project.xml";
  public static final String STEPIK_IDS_JSON = "stepik_ids.json";

  public static final String COURSE_IGNORE = ".courseignore";

  // IDs of supported languages. They are the same that `Language#getID` returns
  // but in some cases we don't have corresponding Language in classpath to get its id via `getID` method
  public static final String JAVA = "JAVA";
  public static final String KOTLIN = "kotlin";
  public static final String PYTHON = "Python";
  public static final String SCALA = "Scala";
  public static final String JAVASCRIPT = "JavaScript";
  public static final String TYPESCRIPT = "TypeScript";
  public static final String RUST = "Rust";
  // Single `ObjectiveC` id is used both for `ObjectiveC` and `C/C++`
  public static final String CPP = "ObjectiveC";
  public static final String GO = "go";
  public static final String PHP = "PHP";

  // Language versions
  public static final String PYTHON_2_VERSION = "2.x";
  public static final String PYTHON_3_VERSION = "3.x";

  // Submissions status
  public static final String CORRECT = "correct";
  public static final String WRONG = "wrong";
  public static final String UNCHECKED = "unchecked";

  // Troubleshooting guide links
  public static final String TROUBLESHOOTING_GUIDE_URL = "https://plugins.jetbrains.com/plugin/10081-edutools/docs/troubleshooting-guide.html";
  public static final String NO_TESTS_URL = TROUBLESHOOTING_GUIDE_URL + "#no_tests_have_run";
  public static final String NO_COURSES_URL = TROUBLESHOOTING_GUIDE_URL + "#no_courses_found";
  public static final String FAILED_TO_POST_TO_JBA_URL = TROUBLESHOOTING_GUIDE_URL + "#failed_submission_jba";
  public static final String FAILED_TO_CHECK_URL = TROUBLESHOOTING_GUIDE_URL + "#failed_to_launch_checking";
  public static final String OUTSIDE_OF_KNOWN_PORT_RANGE_URL = TROUBLESHOOTING_GUIDE_URL + "#outside_of_known_port_range";

  // Help links
  public static final String HELP_URL = "https://www.jetbrains.com/help";
  public static final String LEARNER_START_GUIDE = HELP_URL + "/education/learner-start-guide.html";

  private EduNames() {
  }

}
