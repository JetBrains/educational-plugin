### Let's write some ~~_code_~~ words!

First, find the age of the user's dog in human years.
\
Save the age into a variable named `humanYears` for future use.

Using backticks will make your description clearer and less ambiguous to **Cognifire**. 
Please wrap all variable or function names, or calls to them or any code in `` ` `` (backticks).
This technique is also useful whenever you are interacting with an LLM, for example ChatGPT, and it often may improve your prompt's clarity and will result in a better output!

<div class="hint" title="Example of a prompt">

```kotlin
fun calculateDogAgeInDogYears() {
  prompt("""
        Get the user input and save the result to a variable named `humanYears`.
    """)
}
```
</div>