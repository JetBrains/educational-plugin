package com.jetbrains.edu.coursecreator.yaml


class InvalidYamlFormatException(override val message: String) : IllegalStateException(message)