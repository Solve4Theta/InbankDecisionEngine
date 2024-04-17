package ee.taltech.inbankbackend.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneId;
import ee.taltech.inbankbackend.config.DecisionEngineConstants;

/**
 * A class which validates the user's age.
 * The user has invalid age if they are under 18 or older than the lowest life
 * expectancy with respect to gender in all the supported countries subtracted
 * by our maximum loan period in years.
 */
public class AgeVerification {

    /**
     * Determines if the user is eligible for a loan based on their age.
     *
     * @param personalCode The personal ID code of the user.
     * @return true if the user meets age requirements, false otherwise.
     */
    public boolean isEligibleByAge(String personalCode) {
        LocalDate birthdate = parseBirthDate(personalCode);
        LocalDateTime nowInEstonia = LocalDateTime.now(ZoneId.of("Europe/Tallinn"));
        LocalDate todayInEstonia = nowInEstonia.toLocalDate();
        Period age = Period.between(birthdate, todayInEstonia);

        if (age.getYears() < 18) {
            return false;
        }

        return checkMaxAgeLimit(isMale(personalCode), age);
    }

    /**
     * Parses the user's birthdate from the ID code.
     *
     * @param idCode The identification code of the user.
     * @return LocalDate representing the user's birthdate.
     */
    private LocalDate parseBirthDate(String idCode) {
        int firstDigit = Integer.parseInt(idCode.substring(0, 1));
        int century;
        switch (firstDigit) {
            case 1: case 2: century = 1800; break;
            case 3: case 4: century = 1900; break;
            case 5: case 6: century = 2000; break;
            default: century = 2000; break;
        }

        int year = century + Integer.parseInt(idCode.substring(1, 3));
        int month = Integer.parseInt(idCode.substring(3, 5));
        int day = Integer.parseInt(idCode.substring(5, 7));

        return LocalDate.of(year, month, day);
    }

    /**
     * Determines the gender of the user based on the first digit of their personal ID code.
     *
     * @param personalCode The personal ID code of the user.
     * @return true if the user is male, false otherwise.
     */
    private boolean isMale(String personalCode) {
        int genderCode = Integer.parseInt(personalCode.substring(0, 1));
        return genderCode % 2 != 0;
    }

    /**
     * Checks if the user's age exceeds the maximum allowed age limit.
     *
     * @param isMale Indicates if the user is male.
     * @param userAge The age of the user.
     * @return true if the user's age is within the limit, false otherwise.
     */
    private boolean checkMaxAgeLimit(boolean isMale, Period userAge) {
        String genderId = isMale ? "_M" : "_W";
        for (String key : DecisionEngineConstants.LIFE_EXPECTANCIES.keySet()) {
            if (key.endsWith(genderId)) {
                int lifeExpectancy = DecisionEngineConstants.LIFE_EXPECTANCIES.get(key);
                int adjustedLifeExpectancy = lifeExpectancy - (DecisionEngineConstants.MAXIMUM_LOAN_PERIOD / 12);
                if (adjustedLifeExpectancy < userAge.getYears()) {
                    return false;
                }
            }
        }
        return true;
    }
}
