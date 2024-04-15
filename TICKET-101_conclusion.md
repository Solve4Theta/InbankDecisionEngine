# TICKET-101 Validation

## Strengths
- Correctly calculates the credit modifier based on the last four digits of the personal code.
- Implements understandable code structure 
- Handles cases where a person has debt by throwing a `NoValidLoanException`.
- Validates input parameters according to specific rules.

## Areas for Improvement
- The case where the user has a debt is not covered. More specifically, the credit score is not checked.
- Code organization and readability could be improved for better maintainability.

### Critical problem
In the initial implementation of the decision engine, there was a potential issue that could have led to an infinite loop. The loop in the `calculateApprovedLoan` method incremented the `loanPeriod` until a valid loan amount is found. However, if the `creditModifier` is 0 (indicating the person has debt), the loop will never terminate because the `highestValidLoanAmount` function will always return 0, regardless of the loan period. This situation needed to be addressed to prevent the possibility of an infinite loop.

### Solution
To fix the issue, I added a check before entering the loop to verify if the `creditModifier` is 0. If it is, we should throw a `NoValidLoanException` immediately, indicating that no valid loan can be approved due to the person having debt. Additionally, within the loop, we should check if the `loanPeriod` exceeds the maximum allowed loan period. If it does, we should also throw a `NoValidLoanException` to prevent the loop from running indefinitely. I also added a check for the `outputLoanAmount`, because if `highestValidLoanAmount` returns 0, the user does not have a sufficient credit score in order to apply for a loan. 

### Code changes, with accounting for potential debt and the infinite loop fixed
```java
public Decision calculateApprovedLoan(String personalCode, Long loanAmount, int loanPeriod)
            throws InvalidPersonalCodeException, InvalidLoanAmountException, InvalidLoanPeriodException,
            NoValidLoanException {
        try {
            verifyInputs(personalCode, loanAmount, loanPeriod);
        } catch (Exception e) {
            return new Decision(null, null, e.getMessage());
        }

        creditModifier = getCreditModifier(personalCode);

        if (creditModifier == 0) {
            throw new NoValidLoanException("No valid loan found due to debt!");
        }

        int outputLoanAmount = highestValidLoanAmount(loanAmount, loanPeriod);

        if (outputLoanAmount == 0) {
            throw new NoValidLoanException("No valid loan found!");
        }

        while (outputLoanAmount < DecisionEngineConstants.MINIMUM_LOAN_AMOUNT) {
            loanPeriod++;
            if (loanPeriod > DecisionEngineConstants.MAXIMUM_LOAN_PERIOD) {
                throw new NoValidLoanException("No valid loan found within maximum loan period!");
            }
            outputLoanAmount = highestValidLoanAmount(loanAmount, loanPeriod);
        }

        return new Decision(outputLoanAmount, loanPeriod, null);
    }

    /**
     * Calculates the largest valid loan for the current credit modifier and loan period.
     * @param loanAmount Requested loan amount
     * @param loanPeriod Loan period
     * @return Largest valid loan amount
     */
    private int highestValidLoanAmount(Long loanAmount, int loanPeriod) {
        double creditScore = (double) creditModifier / loanAmount * loanPeriod;
        if (creditScore >= 1) {
            return Math.min(loanAmount.intValue(), DecisionEngineConstants.MAXIMUM_LOAN_AMOUNT);
        }
        return 0;
    }
```

### Minor details
- The form showed the shortest possible loan period as 6 months, so I changed the label to display 12 months.
- I assume that the credit modifier logic is not to be changed, so I just left it as is with the hardcoded logic.


## Conclusion
In conclusion, the implementation of TICKET-101 provides a solid foundation for the decision engine functionality. Although some errors and potential vulnerabilities were present, the overall structure of the code was good and with the provided changes, the decision engine should now function as specified in TICKET-101. 