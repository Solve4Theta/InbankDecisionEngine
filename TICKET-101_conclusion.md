# TICKET-101 Validation

## Strengths
- Correctly calculates the credit modifier based on the last four digits of the personal code.
- Provides good initial idea for calculating maximum possible loan amount.
- Implements understandable code structure. 
- Handles cases where a person has debt by throwing a `NoValidLoanException`.
- Validates input parameters according to specific rules.

## Areas for Improvement
- Although the initial decision engine core provided good grounds for the final solution, the credit score algorithm was not implemented.
- Some comments about specific steps in the code could have made the code more understandable. 

## Most critical shortcoming
- If the decision engine would never take into account the credit score of the user, our organization would be making poorly informed financial decisions. Thus, it is crucial to implement the credit score algorithm. 

### Proposed code changes
```java
float creditScore = 0;
while (true) {
    creditScore = (float)creditModifier / (float)highestValidLoanAmount(loanPeriod) * loanPeriod;
    if (highestValidLoanAmount(loanPeriod) >= DecisionEngineConstants.MINIMUM_LOAN_AMOUNT && creditScore >= 1) {
        break;
    }
    loanPeriod++;
}
```

### Minor details
- The form showed the shortest possible loan period as 6 months, so I changed the label to display 12 months.
- I assume that the credit modifier logic is not to be changed, so I just left it as is with the hardcoded logic.


## Conclusion
In conclusion, the implementation of TICKET-101 provides a solid foundation for the decision engine functionality. Although some errors and potential vulnerabilities were present, the overall structure of the code was good and with the provided changes, the decision engine should now function as specified in TICKET-101. 