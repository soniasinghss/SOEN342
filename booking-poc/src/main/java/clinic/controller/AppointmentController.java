package clinic.controller;

import clinic.domain.Appointment;
import clinic.domain.AvailabilitySlot;
import clinic.domain.BookingException;
import clinic.domain.Clinic;
import clinic.domain.MedicalService;
import clinic.domain.Patient;
import clinic.domain.ResourceAllocation;
import clinic.domain.ResourceRequirement;

import java.util.List;
import java.util.Optional;

/**
 * GRASP Controller (use-case handler) for Book Appointment.
 * Each public method is one system operation from the SSDs.
 */
public class AppointmentController {
    private final Clinic clinic;

    private Appointment current;           // booking in progress

    public AppointmentController(Clinic clinic) {
        this.clinic = clinic;
    }

    /** SO1: startBooking(channel) - a patient has arrived at the desk (WALK_IN) or calls (PHONE). */
    public Appointment startBooking(Appointment.Channel channel) {
        current = clinic.getAppointments().createAppointment(channel);
        return current;
    }

    /** SO2: identifyPatient(ramqOrPhone) - empty result means the patient must be registered. */
    public Optional<Patient> identifyPatient(String ramqOrPhone) {
        requireBooking();
        Optional<Patient> patient = clinic.getPatients().find(ramqOrPhone);
        patient.ifPresent(current::setPatient);
        return patient;
    }

    /** SO2 (alt): registerPatient(...) - new patient; RAMQ and insurance may be null. */
    public Patient registerPatient(String firstName, String lastName, String telephone,
                                   String ramqNo, String insurance) {
        requireBooking();
        Patient patient = clinic.getPatients().register(firstName, lastName, telephone, ramqNo, insurance);
        current.setPatient(patient);
        return patient;
    }

    /** SO4: selectSlot(slotId, serviceName) - returns the resources that will be reserved. */
    public List<ResourceRequirement> selectSlot(int slotId, String serviceName) {
        requireBooking();
        if (current.getPatient() == null) {
            throw new BookingException("Identify the patient before selecting a slot");
        }
        AvailabilitySlot slot = clinic.getSchedule().findSlot(slotId);
        MedicalService service = clinic.findService(serviceName);
        if (!slot.isOpen()) {
            throw new BookingException("Slot #" + slotId + " is no longer available");
        }
        if (clinic.getAppointments().hasConflict(current.getPatient(), slot.getTime())) {
            throw new BookingException(current.getPatient().getFullName()
                    + " already has an appointment at " + slot.getTime());
        }
        if (!clinic.getResources().canSatisfy(service.getRequirements(), slot.getTime())) {
            throw new BookingException("Resources for " + service.getName()
                    + " are not available on " + slot.getTime());
        }
        current.setSlot(slot, service);
        return service.getRequirements();
    }

    /** SO5: confirmBooking() - reserves the resources, books the slot, records the appointment. */
    public Appointment confirmBooking() {
        requireBooking();
        if (current.getSlot() == null) {
            throw new BookingException("Select a slot before confirming");
        }
        List<ResourceAllocation> allocations = clinic.getResources()
                .allocate(current.getService().getRequirements(), current.getTime());
        try {
            current.confirm(allocations);
        } catch (BookingException e) {
            clinic.getResources().release(allocations);
            throw e;
        }
        clinic.getAppointments().add(current);
        Appointment booked = current;
        current = null;
        return booked;
    }

    private void requireBooking() {
        if (current == null) throw new BookingException("Call startBooking first");
    }
}
