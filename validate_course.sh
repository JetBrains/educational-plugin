#!/bin/bash

#/idea/bin/idea.sh validateCourse /project --marketplace $1 -Djava.awt.headless=true -Didea.is.internal=true
#/idea/bin/idea.sh installCoursePlugins /project --marketplace $1 -Djava.awt.headless=true -Didea.is.internal=true
/idea/bin/idea.sh installCoursePlugins /project --marketplace 16628 -Djava.awt.headless=true -Didea.is.internal=true
/idea/bin/idea.sh validateCourse /project --marketplace 16628 -Djava.awt.headless=true -Didea.is.internal=true -Dproject.jdk=/idea/jbr