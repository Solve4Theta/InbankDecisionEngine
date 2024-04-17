# TICKET-101 Validation

## Strengths
- **Correct Calculation of Credit Modifier**: Effectively calculates the credit modifier based on the last four digits of the personal code.
- **Loan Calculation Logic**: Provides a solid initial mechanism for calculating the maximum possible loan amount.
- **Code Structure**: Implements a straightforward and understandable code structure.
- **Exception Handling**: Appropriately handles cases where a person has a low credit score (segment less than 2500) by throwing a `NoValidLoanException`.
- **Input Validation**: Validates input parameters according to specific business rules ensuring robust data handling.

## Areas for Improvement
- **Lack of Credit Score Algorithm**: The current system does not implement a comprehensive credit score algorithm, relying solely on a segment-based credit modifier.
- **Documentation**: Some methods could benefit from more detailed comments to enhance code readability and maintainability.

## Most Critical Shortcoming
- **Credit Scoring Deficiency**: Not considering the broader credit score of the user risks making financially unsound decisions. Integrating a full credit assessment algorithm is crucial for informed lending decisions.

### Proposed Code Changes
```java
while (loanPeriod <= DecisionEngineConstants.MAXIMUM_LOAN_PERIOD) {
    float creditScore = (float)creditModifier / (float)highestValidLoanAmount(loanPeriod) * loanPeriod;
    if (highestValidLoanAmount(loanPeriod) >= DecisionEngineConstants.MINIMUM_LOAN_AMOUNT && creditScore >= 1) {
        outputLoanAmount = Math.min(DecisionEngineConstants.MAXIMUM_LOAN_AMOUNT, highestValidLoanAmount(loanPeriod));
        return new Decision(outputLoanAmount, loanPeriod, null);
    }
    loanPeriod++;
}

// If program flow gets here, we cannot provide a loan for the user
throw new NoValidLoanException("No valid loan found!");
```
### SOLID Principles Evaluation
- **Single Responsibility Principle (SRP)**
    - The DecisionEngine class could further embrace SRP by extracting validation logic into separate classes or methods, reducing its responsibilities.
- **Dependency Inversion Principle (DIP)**
    - The DecisionEngine class directly depends on the EstonianPersonalCodeValidator, which is a specific implementation rather than an abstraction. To better conform to DIP, the code should depend on abstractions (like an interface for ID code validation) rather than on concrete classes.


### Minor details
- The form showed the shortest possible loan period as 6 months, so I changed the label to display 12 months.
- I assume that the credit modifier logic is not to be changed, so I just left it as is with the hardcoded logic.


## Conclusion
In conclusion, the implementation of TICKET-101 provides a solid foundation for the decision engine functionality. Although some errors and potential vulnerabilities were present, the overall structure of the code was good and with the provided changes, the decision engine should now function as specified in TICKET-101. 