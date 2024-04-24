### What if...
Then, **if** the function `verifyHumanYearsInput` returned true, cast `humanYearsInput` to an integer and save it to `humanYears`.\
Calculate the age in dog years and save it to a variable `dogYeas`.\
Output the message "Your dog's age in dog years is: `dogYears`". \
However, if the function `verifyHumanYearsInput` returned false, output  
`Oops! That doesn't look like a valid age.`

<div class="hint" title="How to calculate age in dog years?">

Age in dog years equals human years multiplied by `7`.
</div>

<div class="hint" title="Example of a prompt">

```kotlin
fun calculateDogAgeInDogYears() {
  prompt("""
        Get the user input and save the result to `humanYearsInput`.
        Call the function `verifyHumanYearsInput` with `humanYearsInput`. 
        If the function returns true, set `humanYears` equal to `humanYearsInput` interpreted as an integer.
        Then, initialize `dogYears` to `humanYears` multiplied by 7 and output the message "Your dog's age in dog years is: `dogYears`".
        If the function returns false, output the message "Oops! That doesn't look like a valid age.".
    """)
}
```
</div>