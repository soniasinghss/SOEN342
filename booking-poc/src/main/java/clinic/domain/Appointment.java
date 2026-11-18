package clinic.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * An appointment for a patient in a professional's availability slot.
 * Lifecycle: PENDING (being built by the booking clerk) -> BOOKED.
 */
public class Appointment {

    public enum Status { PENDING, BOOKED }
    public enum Channel { WALK_IN, PHONE }

    private final int id;
    private final Channel channel;
    private Status status = Status.PENDING;
    private Patient patient;
    private AvailabilitySlot slot;
    private MedicalService service;
    private List<ResourceAllocation> allocations = new ArrayList<>();

    public Appointment(int id, Channel channel) {
        this.id = id;
        this.channel = channel;
    }

    public int getId() { return id; }
    public Channel getChannel() { return channel; }
    public Status getStatus() { return status; }
    public Patient getPatient() { return patient; }
    public AvailabilitySlot getSlot() { return slot; }
    public MedicalService getService() { return service; }
    public List<ResourceAllocation> getAllocations() { return List.copyOf(allocations); }

    public boolean isBooked() {
        return status == Status.BOOKED;
    }

    public TimeSlot getTime() {
        return slot.getTime();
    }

    public void setPatient(Patient patient) {
        requireStatus(Status.PENDING);
        this.patient = patient;
    }

    public void setSlot(AvailabilitySlot slot, MedicalService service) {
        requireStatus(Status.PENDING);
        this.slot = slot;
        this.service = service;
    }

    /** confirmBooking: books the slot and keeps the resource allocations. */
    public void confirm(List<ResourceAllocation> allocations) {
        requireStatus(Status.PENDING);
        if (patient == null || slot == null) {
            throw new BookingException("A patient and a slot must be selected before confirming");
        }
        slot.book();
        this.allocations = new ArrayList<>(allocations);
        status = Status.BOOKED;
    }

    private void requireStatus(Status expected) {
        if (status != expected) {
            throw new BookingException("Appointment " + id + " is " + status + ", expected " + expected);
        }
    }

    @Override
    public String toString() {
        return "Appt #" + id + " [" + status + ", " + channel + "] "
                + (patient == null ? "?" : patient.getFullName()) + " with "
                + (slot == null ? "?" : slot.getProfessional() + " on " + slot.getTime())
                + (service == null ? "" : " - " + service.getName())
                + (allocations.isEmpty() ? "" : " resources " + allocations);
    }
}
