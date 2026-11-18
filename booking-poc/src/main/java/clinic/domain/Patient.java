package clinic.domain;

/** A patient. The RAMQ number and insurance are optional (e.g. out-of-province patients). */
public class Patient {
    private final int id;
    private final String firstName;
    private final String lastName;
    private final String telephone;
    private final String ramqNo;
    private final String insurance;

    public Patient(int id, String firstName, String lastName, String telephone, String ramqNo, String insurance) {
        if (telephone == null || telephone.isBlank()) {
            throw new BookingException("A telephone number is required");
        }
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.telephone = telephone;
        this.ramqNo = blankToNull(ramqNo);
        this.insurance = blankToNull(insurance);
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank() || s.equals("-") || s.equals("–")) ? null : s.trim();
    }

    public int getId() { return id; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getFullName() { return firstName + " " + lastName; }
    public String getTelephone() { return telephone; }
    public String getRamqNo() { return ramqNo; }
    public String getInsurance() { return insurance; }

    public boolean hasRamq() {
        return ramqNo != null;
    }

    /** Matches on RAMQ number or telephone number. */
    public boolean isIdentifiedBy(String ramqOrPhone) {
        String key = ramqOrPhone.trim();
        return key.equalsIgnoreCase(ramqNo) || key.equals(telephone);
    }

    @Override
    public String toString() {
        return getFullName() + " (" + telephone + (hasRamq() ? ", RAMQ " + ramqNo : ", no RAMQ") + ")";
    }
}
