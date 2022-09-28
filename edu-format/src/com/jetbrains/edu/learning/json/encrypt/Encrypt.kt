package com.jetbrains.edu.learning.json.encrypt

import com.fasterxml.jackson.annotation.JacksonAnnotation

@JacksonAnnotation
@Target(AnnotationTarget.FIELD, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER,
        AnnotationTarget.VALUE_PARAMETER)
annotation class Encrypt