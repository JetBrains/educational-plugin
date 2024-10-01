package com.jetbrains.edu.cognifire.validation

/**
 * A class that represents an action to validate generated code from bad prompts.
 * This class extends [GeneratedCodeValidationAction] and is specifically configured to use bad prompts for the validation process.
 */
class GeneratedCodeFromBadPromptsValidationAction : GeneratedCodeValidationAction(true)