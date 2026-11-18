package clinic.domain;

import java.util.ArrayList;
import java.util.List;

/** All appointments made in the clinic. */
public class AppointmentBook {
    private final List<Appointment> appointments = new ArrayList<>();
    private int nextId = 1;

    /** Creator: the book records appointments, so it creates them (status PENDING). */
    public Appointment createAppointment(Appointment.Channel channel) {
        return new Appointment(nextId++, channel);
    }

    public void add(Appointment appointment) {
        appointments.add(appointment);
    }

    public Appointment find(int appointmentId) {
        return appointments.stream().filter(a -> a.getId() == appointmentId).findFirst()
                .orElseThrow(() -> new BookingException("No appointment #" + appointmentId));
    }

    public List<Appointment> findBooked(Patient patient) {
        return appointments.stream().filter(a -> a.isBooked() && a.getPatient() == patient).toList();
    }

    /** Business rule: a patient cannot have two booked appointments at overlapping times. */
    public boolean hasConflict(Patient patient, TimeSlot time) {
        return findBooked(patient).stream().anyMatch(a -> a.getTime().overlaps(time));
    }

    public List<Appointment> getAll() {
        return List.copyOf(appointments);
    }
}
