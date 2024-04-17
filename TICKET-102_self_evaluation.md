
## TICKET-102 Self Evaluation

### Age-Related Additions 

This section provides a overview of the logic integrated into the `DecisionEngine` (and ultimately into the separate `AgeVerification` class) class to evaluate the age suitability of users applying for a loan, based on their personal ID code.

#### 1. Minimum Age Check
- **Purpose**: Ensures compliance with legal age requirements for entering into contractual agreements such as loans.
- **Implementation Details**:
    - The age is calculated using the `Period` class to measure the difference between the parsed birthdate from the customer's personal ID code and today's date in Estonia. 
      - Of course this would have to be re-evaluated if the application is meant to be used in multiple timezones.
    - If the calculated age is less than 18 years, the loan application is denied outright. 
- **Relevant Code Snippet**:
  ```java
  if (age.getYears() < 18) {
      return false;
  }

#### 2. Maximum Age Check
- **Purpose**: To prevent issuing loans to individuals who may not live through the payback period, potentially based on actuarial life expectancies.
- **Implementation Details**:
    - The system uses a pre-defined map (`DecisionEngineConstants.LIFE_EXPECTANCIES`) that stores life expectancies differentiated by gender identifiers (`_M` for males, `_W` for females).
    - The maximum age limit for loan approval is calculated by subtracting the loan payback period (converted from months to years) from these life expectancies.
    - If a customer's current age exceeds the adjusted life expectancy for their gender, the loan application is denied.
- **Relevant Code Snippet**:
  ```java
  private boolean checkMaxAgeLimit(boolean isMale, Period userAge) {
      String genderId = isMale ? "_M" : "_W";
      boolean approveResult = true;
      for (String key : DecisionEngineConstants.LIFE_EXPECTANCIES.keySet()) {
          if (key.endsWith(genderId)) {
              int lifeExpectancy = DecisionEngineConstants.LIFE_EXPECTANCIES.get(key);
              int adjustedLifeExpectancy = lifeExpectancy - (DecisionEngineConstants.MAXIMUM_LOAN_PERIOD / 12);
              if (adjustedLifeExpectancy < userAge.getYears()) {
                  approveResult = false;
              }
          }
      }
      return approveResult;
  }
  
### SOLID
- **In order to** adhere to `SOLID` principles, I have moved all the logic that has to do with age verification to a separate `AgeVerification` class, which is instantiated in the `calculateApproveLoan` function of the `DecisionEngine` class.
- **Not only** does this respect the `Single Responsibility Principle`, but it also makes the code more modular which has to do with the `Open/Closed Principle`, allowing for new functionality to be added without delving into the details of altering old code.
- **Interface Segregation Principle** - At some point it would be wise to segregate the `DecisionEngine` class and the `AgeVerification` class into different interfaces or manage both classes in the `DecisionEngineController`, but currently this would just mean a lot of boilerplate and not much functionality to gain.


## Things to consider
- Since we cannot determine the nationality of the user from their id code and we have to implement Baltic scope, we check for all the life expectancies of the supported countries with respect to gender. This means that when someone from Estonia applies for a loan which they are eligible for, according to Estonian life expectancy, the loan is still denied for them if they are not eligible in Latvia or Lithuania. 
- The life expectancies of the currently included countries (Baltic countries) are arbitrarily chosen and are not connected to any real world data.
- Currently, the life expectancies are stored in a <String, Integer> key value map. For scaling the application, this should be re-evaluated in favor of a more dynamic approach. Perhaps some script that pulls real world data from some API.
- Upper age limit calculations are done with integers, because the current maximum loan period is 60 months, which in years is exactly 5. If maximum loan period changes, calculation variables should be re-evaluated so that calculation errors due to precision loss of using integers would be avoided.