package clinic.domain;

/**
 * One bookable block of a professional's time. Created by "Make Availability"
 * or "Import from CSV"; booked through "Book Appointment".
 */
public class AvailabilitySlot {

    public enum Status { OPEN, BOOKED }

    private final int id;
    private final Professional professional;
    private final TimeSlot time;
    private Status status = Status.OPEN;

    public AvailabilitySlot(int id, Professional professional, TimeSlot time) {
        this.id = id;
        this.professional = professional;
        this.time = time;
    }

    public int getId() { return id; }
    public Professional getProfessional() { return professional; }
    public TimeSlot getTime() { return time; }
    public Status getStatus() { return status; }

    public boolean isOpen() {
        return status == Status.OPEN;
    }

    /** Information Expert: the slot decides whether it satisfies the criteria. */
    public boolean matches(SearchCriteria c) {
        if (c.specialty() != null && !professional.getSpecialty().equalsIgnoreCase(c.specialty())) return false;
        if (c.professionalName() != null && !professional.hasName(c.professionalName())) return false;
        return time.isWithin(c.fromDate(), c.toDate());
    }

    public void book() {
        if (!isOpen()) throw new BookingException("Slot " + id + " is already booked");
        status = Status.BOOKED;
    }

    @Override
    public String toString() {
        return "#" + id + " " + time + " " + professional + " [" + status + "]";
    }
}
