package clinic.ui;

import clinic.controller.AppointmentController;
import clinic.controller.AvailabilitySearchController;
import clinic.domain.Appointment;
import clinic.domain.AvailabilitySlot;
import clinic.domain.BookingException;
import clinic.domain.Clinic;
import clinic.domain.SearchCriteria;

import java.io.PrintStream;
import java.time.LocalDate;
import java.util.List;

/** Scripted walkthrough of the PoC scenarios, for the demo / TA. */
final class Demo {
    private Demo() { }

    static void run(Clinic clinic, PrintStream out) {
        AppointmentController booking = new AppointmentController(clinic);
        AvailabilitySearchController search = new AvailabilitySearchController(clinic);
        LocalDate nov2 = LocalDate.of(2026, 11, 2);

        out.println("== 1. Search Availability: Cardiology on 2026-11-02 (any service)");
        SearchCriteria cardio = SearchCriteria.any().withSpecialty("Cardiology").between(nov2, nov2);
        print(out, search.searchAvailability(cardio));

        out.println("\n== 2. Same search, but for an ECG (needs ECG Machine + Nurse)");
        out.println("   10:00 disappears: the only ECG machine is taken by Olivier Côté's ECG.");
        print(out, search.searchAvailability(cardio.forService("ECG")));

        out.println("\n== 3. Book (walk-in): Alexandre Dubois arrives at the desk (known patient, RAMQ)");
        booking.startBooking(Appointment.Channel.WALK_IN);
        out.println("   identifyPatient -> " + booking.identifyPatient("DUBA87050805").orElseThrow());
        int ecgSlot = search.searchAvailability(cardio.forService("ECG")).get(0).getId();
        out.println("   selectSlot(#" + ecgSlot + ", ECG) -> will reserve " + booking.selectSlot(ecgSlot, "ECG"));
        Appointment walkIn = booking.confirmBooking();
        out.println("   confirmBooking -> " + walkIn);

        out.println("\n== 4. Book (phone): new patient without RAMQ (Lucas Martin)");
        booking.startBooking(Appointment.Channel.PHONE);
        out.println("   identifyPatient(514-555-0300) -> "
                + (booking.identifyPatient("514-555-0300").isPresent() ? "found" : "not found, so registerPatient"));
        out.println("   registerPatient -> " + booking.registerPatient("Lucas", "Martin", "514-555-0300", null, "Sun Life"));
        int famSlot = search.searchAvailability(SearchCriteria.any().withProfessional("Tremblay")).get(0).getId();
        booking.selectSlot(famSlot, "Consultation");
        Appointment phone = booking.confirmBooking();
        out.println("   confirmBooking -> " + phone);

        out.println("\n== 5. Rejected: booking an already booked slot");
        booking.startBooking(Appointment.Channel.PHONE);
        booking.identifyPatient("MORJ94031404");
        try {
            booking.selectSlot(famSlot, "Consultation");
        } catch (BookingException e) {
            out.println("   ! " + e.getMessage());
        }

        out.println("\n== Final appointment book");
        clinic.getAppointments().getAll().forEach(a -> out.println("   " + a));
    }

    private static void print(PrintStream out, List<AvailabilitySlot> slots) {
        if (slots.isEmpty()) out.println("   (none)");
        slots.forEach(s -> out.println("   " + s));
    }
}
