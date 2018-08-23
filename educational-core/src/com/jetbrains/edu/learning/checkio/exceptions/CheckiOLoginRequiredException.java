package com.jetbrains.edu.learning.checkio.exceptions;

import com.jetbrains.edu.learning.checkio.connectors.CheckiOOAuthConnector;

/**
 * It's thrown when an action is performed
 * that requires the user to be logged in, but the user is logged out.
 *
 * @see CheckiOOAuthConnector#requireUserLoggedIn()
 * */
public class CheckiOLoginRequiredException extends Exception { }
