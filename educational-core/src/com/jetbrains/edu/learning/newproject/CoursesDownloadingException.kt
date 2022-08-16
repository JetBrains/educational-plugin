package com.jetbrains.edu.learning.newproject

import java.io.IOException

class CoursesDownloadingException(message: String? = null, val uiMessage: String) : IOException(message)