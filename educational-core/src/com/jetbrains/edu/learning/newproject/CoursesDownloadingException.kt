package com.jetbrains.edu.learning.newproject

import java.io.IOException

class CoursesDownloadingException(val uiMessage: String, message: String? = null) : IOException(message)