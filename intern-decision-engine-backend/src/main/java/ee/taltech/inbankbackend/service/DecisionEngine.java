
package ee.taltech.inbankbackend.service;

import com.github.vladislavgoltjajev.personalcode.locale.estonia.EstonianPersonalCodeValidator;
import ee.taltech.inbankbackend.config.DecisionEngineConstants;
import ee.taltech.inbankbackend.exceptions.InvalidLoanAmountException;
import ee.taltech.inbankbackend.exceptions.InvalidLoanPeriodException;
import ee.taltech.inbankbackend.exceptions.InvalidPersonalCodeException;
import ee.taltech.inbankbackend.exceptions.NoValidLoanException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.Period;

/**
 * A service class that provides a method for calculating an approved loan amount and period for a customer.
 * The loan amount is calculated based on the customer's credit modifier,
 * which is determined by the last four digits of their ID code.
 */
@Service
public class DecisionEngine {

    // Used to check for the validity of the presented ID code.
    private final EstonianPersonalCodeValidator validator = new EstonianPersonalCodeValidator();
    private int creditModifier = 0;
    private int firstDigit = 0;

    /**
     * Calculates the maximum loan amount and period for the customer based on their ID code,
     * the requested loan amount and the loan period.
     * The loan period must be between 12 and 60 months (inclusive).
     * The loan amount must be between 2000 and 10000€ months (inclusive).
     *
     * @param personalCode ID code of the customer that made the request.
     * @param loanAmount Requested loan amount
     * @param loanPeriod Requested loan period
     * @return A Decision object containing the approved loan amount and period, and an error message (if any)
     * @throws InvalidPersonalCodeException If the provided personal ID code is invalid
     * @throws InvalidLoanAmountException If the requested loan amount is invalid
     * @throws InvalidLoanPeriodException If the requested loan period is invalid
     * @throws NoValidLoanException If there is no valid loan found for the given ID code, loan amount and loan period
     */
    public Decision calculateApprovedLoan(String personalCode, Long loanAmount, int loanPeriod)
            throws InvalidPersonalCodeException, InvalidLoanAmountException, InvalidLoanPeriodException,
            NoValidLoanException {
        try {
            verifyInputs(personalCode, loanAmount, loanPeriod);
        } catch (Exception e) {
            return new Decision(null, null, e.getMessage());
        }

        LocalDate birthdate = parseBirthDate(personalCode, firstDigit);
        LocalDateTime nowInEstonia = LocalDateTime.now(ZoneId.of("Europe/Tallinn"));
        LocalDate todayInEstonia = nowInEstonia.toLocalDate();
        Period age = Period.between(birthdate, todayInEstonia);

        if (age.getYears() < 18) {
            return new Decision(null, null, "Loan denied: Age falls below minimum allowed limit.");
        }

        boolean isMale = isMale(personalCode);
        boolean maxAgeLimitResult = checkMaxAgeLimit(isMale, age);

        if (!maxAgeLimitResult) {
            return new Decision(null, null, "Loan denied: Age exceeds maximum allowed limit.");
        }

        int outputLoanAmount = 0;
        creditModifier = getCreditModifier(personalCode);

        if (creditModifier == 0) {
            throw new NoValidLoanException("No valid loan found!");
        }

        while (true) {
            float creditScore = (float)creditModifier / (float)highestValidLoanAmount(loanPeriod) * loanPeriod;
            if (highestValidLoanAmount(loanPeriod) >= DecisionEngineConstants.MINIMUM_LOAN_AMOUNT && creditScore >= 1) {
                break;
            }
            loanPeriod++;
        }

        if (loanPeriod <= DecisionEngineConstants.MAXIMUM_LOAN_PERIOD) {
            outputLoanAmount = Math.min(DecisionEngineConstants.MAXIMUM_LOAN_AMOUNT, highestValidLoanAmount(loanPeriod));
        } else {
            throw new NoValidLoanException("No valid loan found!");
        }

        return new Decision(outputLoanAmount, loanPeriod, null);
    }

    /**
     * Checks if user is male
     *
     * @param personalCode Used to identify the gender of the user
     * @return Boolean which is true if user is male and vice versa
     */
    private boolean isMale(String personalCode) {
        int genderCode = Integer.parseInt(personalCode.substring(0, 1));
        return genderCode % 2 != 0;
    }

    /**
     * Checks if user is over maximum allowed age limit
     *
     * @param isMale Used to determine the gender of the user
     * @param userAge Used to determine if user is of eligible age
     * @return Boolean which either approves or denies the user age
     */
    private boolean checkMaxAgeLimit(boolean isMale, Period userAge) {
        String genderId= isMale ? "_M" : "_W";
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


    /**
     * Parses the id code of the user int LocalDate format.
     *
     * @param idCode The identification code of the user.
     * @return user birthdate.
     */
    public static LocalDate parseBirthDate(String idCode, int firstDigit) {
        firstDigit = Integer.parseInt(idCode.substring(0, 1));
        int century;
        switch (firstDigit) {
            case 1: case 2: century = 1800; break;
            case 3: case 4: century = 1900; break;
            case 5: case 6: century = 2000; break;
            // Should not get here, since the id code is already validated
            default: century = 2000; break;
        }

        int year = century + Integer.parseInt(idCode.substring(1, 3));
        int month = Integer.parseInt(idCode.substring(3, 5));
        int day = Integer.parseInt(idCode.substring(5, 7));

        LocalDate birthDate = LocalDate.of(year, month, day);
        return birthDate;
    }

    /**
     * Calculates the largest valid loan for the current credit modifier and loan period.
     *
     * @param loanPeriod The amount of months the user wishes to pay the loan back.
     * @return Largest valid loan amount.
     */
    private int highestValidLoanAmount(int loanPeriod) {
        return creditModifier * loanPeriod;
    }

    /**
     * Calculates the credit modifier of the customer to according to the last four digits of their ID code.
     * Debt - 0000...2499
     * Segment 1 - 2500...4999
     * Segment 2 - 5000...7499
     * Segment 3 - 7500...9999
     *
     * @param personalCode ID code of the customer that made the request.
     * @return Segment to which the customer belongs.
     */
    private int getCreditModifier(String personalCode) {
        int segment = Integer.parseInt(personalCode.substring(personalCode.length() - 4));

        if (segment < 2500) {
            return 0;
        } else if (segment < 5000) {
            return DecisionEngineConstants.SEGMENT_1_CREDIT_MODIFIER;
        } else if (segment < 7500) {
            return DecisionEngineConstants.SEGMENT_2_CREDIT_MODIFIER;
        }

        return DecisionEngineConstants.SEGMENT_3_CREDIT_MODIFIER;
    }

    /**
     * Verify that all inputs are valid according to business rules.
     * If inputs are invalid, then throws corresponding exceptions.
     *
     * @param personalCode Provided personal ID code
     * @param loanAmount Requested loan amount
     * @param loanPeriod Requested loan period
     * @throws InvalidPersonalCodeException If the provided personal ID code is invalid
     * @throws InvalidLoanAmountException If the requested loan amount is invalid
     * @throws InvalidLoanPeriodException If the requested loan period is invalid
     */
    private void verifyInputs(String personalCode, Long loanAmount, int loanPeriod)
            throws InvalidPersonalCodeException, InvalidLoanAmountException, InvalidLoanPeriodException {

        if (!validator.isValid(personalCode)) {
            throw new InvalidPersonalCodeException("Invalid personal ID code!");
        }
        if (!(DecisionEngineConstants.MINIMUM_LOAN_AMOUNT <= loanAmount)
                || !(loanAmount <= DecisionEngineConstants.MAXIMUM_LOAN_AMOUNT)) {
            throw new InvalidLoanAmountException("Invalid loan amount!");
        }
        if (!(DecisionEngineConstants.MINIMUM_LOAN_PERIOD <= loanPeriod)
                || !(loanPeriod <= DecisionEngineConstants.MAXIMUM_LOAN_PERIOD)) {
            throw new InvalidLoanPeriodException("Invalid loan period!");
        }

    }
}
