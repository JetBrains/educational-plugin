package com.jetbrains.edu.cpp

fun getExpectedTaskCMakeText(expectedProjectName: String) = """
    |${cMakeMinimumRequired}
    |project(${expectedProjectName})
    |
    |set(CMAKE_CXX_STANDARD 14)
    |
    |# Files for the task.
    |set(SOURCE
    |        src/task.cpp)
    |
    |# Files for testing.
    |set(TEST
    |        test/test.cpp)
    |
    |set(RUN
    |        src/run.cpp)
    |
    |
    |# Customize student-run target.
    |add_executable(${expectedProjectName}-src
    |        ${'$'}{SOURCE}
    |        ${'$'}{RUN})
    |
    |
    |# Customize test target.
    |add_executable(${expectedProjectName}-test
    |        ${'$'}{SOURCE}
    |        ${'$'}{TEST})
    |
    |target_link_libraries(${expectedProjectName}-test gtest_main)
  """.trimMargin("|")