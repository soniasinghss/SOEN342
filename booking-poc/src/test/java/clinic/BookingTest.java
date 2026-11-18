package clinic;

import clinic.controller.AppointmentController;
import clinic.controller.AvailabilitySearchController;
import clinic.domain.Appointment;
import clinic.domain.AvailabilitySlot;
import clinic.domain.BookingException;
import clinic.domain.Clinic;
import clinic.domain.Patient;
import clinic.domain.SearchCriteria;
import clinic.ui.SampleData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookingTest {

    private static final LocalDate NOV2 = LocalDate.of(2026, 11, 2);

    private Clinic clinic;
    private AppointmentController booking;
    private AvailabilitySearchController search;

    @BeforeEach
    void setUp() {
        clinic = SampleData.load();
        booking = new AppointmentController(clinic);
        search = new AvailabilitySearchController(clinic);
    }

    private AvailabilitySlot slot(String professional, String date, String start) {
        return clinic.getSchedule().getAllSlots().stream()
                .filter(s -> s.getProfessional().hasName(professional)
                        && s.getTime().date().toString().equals(date)
                        && s.getTime().start().toString().equals(start))
                .findFirst().orElseThrow();
    }

    // ------------------------------------------------------------ Search Availability

    @Test
    void searchReturnsOnlyOpenSlotsMatchingSpecialtyAndDates() {
        List<AvailabilitySlot> result = search.searchAvailability(
                SearchCriteria.any().withSpecialty("Cardiology").between(NOV2, NOV2));
        // Bouchard 10:00 is booked (Côté); Pelletier 10:00 and 11:00 are open
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(AvailabilitySlot::isOpen));
        assertTrue(result.stream().allMatch(s -> s.getProfessional().getLastName().equals("Pelletier")));
    }

    @Test
    void searchForServiceHidesSlotsWhoseResourcesAreTaken() {
        List<AvailabilitySlot> result = search.searchAvailability(
                SearchCriteria.any().withSpecialty("Cardiology").between(NOV2, NOV2).forService("ECG"));
        assertEquals(1, result.size(), "10:00 must be hidden: the only ECG machine is in use");
        assertEquals("11:00", result.get(0).getTime().start().toString());
    }

    @Test
    void searchByProfessionalNameIsChronological() {
        List<AvailabilitySlot> result = search.searchAvailability(SearchCriteria.any().withProfessional("Tremblay"));
        assertEquals(List.of("09:30", "10:00"),
                result.stream().map(s -> s.getTime().start().toString()).toList());
    }

    // ------------------------------------------------------------ Book Appointment

    @Test
    void walkInBookingOfKnownPatient() {
        booking.startBooking(Appointment.Channel.WALK_IN);
        Patient p = booking.identifyPatient("DUBA87050805").orElseThrow();
        AvailabilitySlot s = slot("Pelletier", "2026-11-02", "11:00");
        assertEquals(2, booking.selectSlot(s.getId(), "ECG").size());

        Appointment a = booking.confirmBooking();

        assertEquals(Appointment.Status.BOOKED, a.getStatus());
        assertEquals(Appointment.Channel.WALK_IN, a.getChannel());
        assertEquals(p, a.getPatient());
        assertFalse(s.isOpen());
        assertEquals(2, a.getAllocations().size());
        assertTrue(clinic.getAppointments().getAll().contains(a));
    }

    @Test
    void phoneBookingOfNewPatientWithoutRamq() {
        booking.startBooking(Appointment.Channel.PHONE);
        assertTrue(booking.identifyPatient("514-555-0300").isEmpty());
        Patient p = booking.registerPatient("Lucas", "Martin", "514-555-0300", null, null);
        assertFalse(p.hasRamq());
        booking.selectSlot(slot("Tremblay", "2026-11-02", "09:30").getId(), "Consultation");
        Appointment a = booking.confirmBooking();
        assertEquals(Appointment.Channel.PHONE, a.getChannel());
        assertTrue(a.getAllocations().isEmpty());
        // later identified by phone
        booking.startBooking(Appointment.Channel.PHONE);
        assertEquals(p, booking.identifyPatient("514-555-0300").orElseThrow());
    }

    @Test
    void cannotBookABookedSlot() {
        booking.startBooking(Appointment.Channel.WALK_IN);
        booking.identifyPatient("MORJ94031404");
        int taken = slot("Tremblay", "2026-11-02", "09:00").getId(); // Émilie Lavoie
        assertThrows(BookingException.class, () -> booking.selectSlot(taken, "Consultation"));
    }

    @Test
    void cannotSelectSlotWhenResourcesAreTaken() {
        booking.startBooking(Appointment.Channel.WALK_IN);
        booking.identifyPatient("DUBA87050805");
        int pelletier10 = slot("Pelletier", "2026-11-02", "10:00").getId();
        assertThrows(BookingException.class, () -> booking.selectSlot(pelletier10, "ECG"));
        // same slot is fine for a consultation (no resources)
        assertTrue(booking.selectSlot(pelletier10, "Consultation").isEmpty());
    }

    @Test
    void roleCapacityAllowsTwoNursesAtOnceButNotThree() {
        // Nurse capacity is 2; Côté's ECG already uses one at 10:00 on Nov 2.
        clinic.getResources().allocate(
                clinic.findService("Skin Biopsy").getRequirements(),
                slot("Pelletier", "2026-11-02", "10:00").getTime()); // second nurse
        assertEquals(0, clinic.getResources().freeUnits(
                clinic.getResources().findResource("Nurse").orElseThrow(),
                slot("Pelletier", "2026-11-02", "10:00").getTime()));
        // a third nurse at that time is refused
        assertThrows(BookingException.class, () -> clinic.getResources().allocate(
                clinic.findService("ECG").getRequirements(),
                slot("Tremblay", "2026-11-02", "10:00").getTime()));
    }

    @Test
    void patientCannotHaveTwoOverlappingAppointments() {
        booking.startBooking(Appointment.Channel.PHONE);
        booking.identifyPatient("DUBA87050805");
        booking.selectSlot(slot("Pelletier", "2026-11-02", "10:00").getId(), "Consultation");
        booking.confirmBooking();

        booking.startBooking(Appointment.Channel.PHONE);
        booking.identifyPatient("DUBA87050805");
        int sameTime = slot("Tremblay", "2026-11-02", "10:00").getId();
        assertThrows(BookingException.class, () -> booking.selectSlot(sameTime, "Consultation"));
    }

    @Test
    void confirmWithoutSlotFails() {
        booking.startBooking(Appointment.Channel.WALK_IN);
        booking.identifyPatient("DUBA87050805");
        assertThrows(BookingException.class, booking::confirmBooking);
    }

    @Test
    void operationsBeforeStartBookingFail() {
        assertThrows(BookingException.class, () -> booking.identifyPatient("DUBA87050805"));
    }
}
