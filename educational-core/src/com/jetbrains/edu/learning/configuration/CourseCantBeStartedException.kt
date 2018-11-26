package com.jetbrains.edu.learning.configuration

import com.jetbrains.edu.learning.newproject.ui.ErrorState

class CourseCantBeStartedException(val error: ErrorState): Exception()