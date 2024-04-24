### Let's write some ~~_code_~~ words!

First, find the age of the user's dog in human years.
\
Save the age into a variable named `humanYearsInput` for future use.

Using backticks will make your description clearer and less ambiguous to **Cognifire**.
Please wrap all variable or function names in backticks (`` ` ``). \
For a variable named **foo**, we will write `` `foo` `` in the prompt.

<div class="hint" title="Pro tip">
This technique is also useful whenever you are interacting with any Large Language Models, for example, ChatGPT.
It will often improve your prompt's clarity and result in a better output!
</div>
<div class="hint" title="Example of a prompt">

```kotlin
fun calculateDogAgeInDogYears() {
  prompt("""
        Get the user input and save the result to `humanYearsInput`.
    """)
}
```
</div>