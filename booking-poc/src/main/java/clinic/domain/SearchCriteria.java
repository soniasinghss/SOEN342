package clinic.domain;

import java.time.LocalDate;

/**
 * Parameters of searchAvailability. Every field is optional (null = no filter).
 * serviceName, when given, restricts results to slots whose required
 * resources are also free.
 */
public record SearchCriteria(String specialty, String professionalName,
                             LocalDate fromDate, LocalDate toDate, String serviceName) {

    public static SearchCriteria any() {
        return new SearchCriteria(null, null, null, null, null);
    }

    public SearchCriteria withSpecialty(String s) {
        return new SearchCriteria(s, professionalName, fromDate, toDate, serviceName);
    }

    public SearchCriteria withProfessional(String p) {
        return new SearchCriteria(specialty, p, fromDate, toDate, serviceName);
    }

    public SearchCriteria between(LocalDate from, LocalDate to) {
        return new SearchCriteria(specialty, professionalName, from, to, serviceName);
    }

    public SearchCriteria forService(String s) {
        return new SearchCriteria(specialty, professionalName, fromDate, toDate, s);
    }
}
