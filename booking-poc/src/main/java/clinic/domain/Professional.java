package clinic.domain;

/** A healthcare professional (e.g. "Dr. Amélie Tremblay, Family Medicine"). */
public class Professional {
    private final String id;
    private final String firstName;
    private final String lastName;
    private final String title;
    private final String specialty;

    public Professional(String id, String firstName, String lastName, String title, String specialty) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.title = title;
        this.specialty = specialty;
    }

    public String getId() { return id; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getTitle() { return title; }
    public String getSpecialty() { return specialty; }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    /** Case-insensitive match on full name or last name. */
    public boolean hasName(String name) {
        return getFullName().equalsIgnoreCase(name.trim()) || lastName.equalsIgnoreCase(name.trim());
    }

    @Override
    public String toString() {
        return title + " " + getFullName() + " (" + specialty + ")";
    }
}
