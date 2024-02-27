This is the final step before implementing the main function for the game! Let's go!

### Task

Implement the `getRoundResults` function,
which accepts a string `secret` - the secret of the current round of the game, 
a char `guess` - the current guess from the user, and a string `currentUserWord` - the current state of the game, e.g., `_ _ _ K`.
This function should check if the user correctly guessed a char.

<div class="hint" title="Click me to see the new signature of the safeUserInput function">

The signature of the function is:
```kotlin
fun getRoundResults(secret: String, guess: Char, currentUserWord: String): String
```
</div>

This function should have the following behavior:
- inform the user if `guess` is not in the `secret`:
  ```text
  Sorry, the secret does not contain the symbol: <guess>. The current word is <currentUserWord>
  ```
  Here, instead of `<guess>` and `<currentUserWord>`, you need to print the values from the `guess` and `currentUserWord` function arguments: 
  e.g., if the `guess` value is `A`, and the `currentUserWord` is `_ _ _ K`,
  the text `Sorry, the secret does not contain the symbol: A. The current word is _ _ _ K` will be printed.

- inform the user if the `secret` contains the `guess`:
  ```text
  Great! This letter is in the word! The current word is <newUserWord>
  ```
  Here, instead of `<newUserWord>`, you need to print the result from the `generateNewUserWord` function that you implemented earlier: e.g., 
  if the `generateNewUserWord` function returns `B _ _ K`,
  the text `Great! This letter is in the word! The current word is B _ _ K` will be printed.

**Note**: to avoid typos, just copy the text from here and paste it into your code.
