package clinic.ui;

import clinic.controller.AppointmentController;
import clinic.domain.Appointment;
import clinic.domain.Clinic;
import clinic.domain.Professional;
import clinic.domain.Resource;
import clinic.domain.ResourcePool;
import clinic.domain.ResourceRequirement;
import clinic.domain.TimeSlot;

import java.util.List;

/**
 * Stand-in for "Import from CSV" so the booking PoC runs on its own.
 * Rows come from the Iteration II sample CSVs; rows marked (added) create a
 * resource conflict (two cardiologists at 10:00, one ECG machine).
 * Replace load() with the team's CSV importer when merging.
 */
public final class SampleData {

    private SampleData() { }

    private static final String[][] AVAILABILITY = {
            // first, last, title, specialty, date, start, end
            {"Amélie", "Tremblay", "Dr.", "Family Medicine", "2026-11-02", "09:00", "09:30"},
            {"Amélie", "Tremblay", "Dr.", "Family Medicine", "2026-11-02", "09:30", "10:00"},
            {"Marc", "Bouchard", "Dr.", "Cardiology", "2026-11-02", "10:00", "11:00"},
            {"Sophie", "Gagnon", "Nurse Practitioner", "Primary Care", "2026-11-02", "11:00", "11:30"},
            {"Nadia", "Haddad", "Dr.", "Dermatology", "2026-11-03", "13:00", "13:30"},
            {"Nadia", "Haddad", "Dr.", "Dermatology", "2026-11-03", "13:30", "14:00"},
            {"David", "Roy", "Dr.", "Orthopedics", "2026-11-04", "08:00", "08:45"},
            {"Marc", "Bouchard", "Dr.", "Cardiology", "2026-11-04", "14:00", "15:00"},
            {"Julien", "Pelletier", "Dr.", "Cardiology", "2026-11-02", "10:00", "11:00"},   // (added)
            {"Julien", "Pelletier", "Dr.", "Cardiology", "2026-11-02", "11:00", "12:00"},   // (added)
            {"Amélie", "Tremblay", "Dr.", "Family Medicine", "2026-11-02", "10:00", "10:30"}, // (added)
            {"Nadia", "Haddad", "Dr.", "Dermatology", "2026-11-03", "14:00", "14:30"},       // (added)
            {"David", "Roy", "Dr.", "Orthopedics", "2026-11-04", "08:45", "09:30"},          // (added)
    };

    private static final String[][] PATIENTS = {
            // first, last, telephone, RAMQ, insurance  (waiting-list sample file)
            {"Juliette", "Morin", "514-555-0201", "MORJ94031404", null},
            {"Alexandre", "Dubois", "514-555-0202", "DUBA87050805", "Manulife"},
            {"Maya", "Nguyen", "438-555-0203", null, "Blue Cross"},
            {"Gabriel", "Fortin", "514-555-0204", "FORG78092306", null},
            {"Chloé", "Bélanger", "438-555-0205", "BELC01011207", null},
    };

    private static final String[][] BOOKED = {
            // professional, date, start, patient first, last, telephone, RAMQ, service
            {"Amélie Tremblay", "2026-11-02", "09:00", "Émilie", "Lavoie", "514-555-0101", "LAVE99010101", "Consultation"},
            {"Marc Bouchard", "2026-11-02", "10:00", "Olivier", "Côté", "514-555-0102", "COTO85061502", "ECG"},
            {"Nadia Haddad", "2026-11-03", "13:00", "Fatima", "Benali", "438-555-0103", null, "Consultation"},
            {"David Roy", "2026-11-04", "08:00", "Thomas", "Girard", "514-555-0104", "GIRT92082403", "Consultation"},
    };

    public static Clinic load() {
        Clinic clinic = new Clinic();

        // predefined resources; ROLE capacity = how many people in that role are on shift
        ResourcePool pool = clinic.getResources();
        Resource procRoom = pool.addResource("Procedure Room", Resource.Kind.ROOM, 1);
        Resource castRoom = pool.addResource("Cast Room", Resource.Kind.ROOM, 1);
        Resource ecg = pool.addResource("ECG Machine", Resource.Kind.EQUIPMENT, 1);
        Resource nurse = pool.addResource("Nurse", Resource.Kind.ROLE, 2);
        Resource tech = pool.addResource("Orthopedic Technician", Resource.Kind.ROLE, 1);

        clinic.addService("Consultation", List.of());
        clinic.addService("ECG", List.of(new ResourceRequirement(ecg, 1), new ResourceRequirement(nurse, 1)));
        clinic.addService("Skin Biopsy", List.of(new ResourceRequirement(procRoom, 1), new ResourceRequirement(nurse, 1)));
        clinic.addService("Cast Application", List.of(new ResourceRequirement(castRoom, 1), new ResourceRequirement(tech, 1)));

        for (String[] r : AVAILABILITY) {
            Professional p = clinic.findProfessional(r[0] + " " + r[1])
                    .orElseGet(() -> clinic.addProfessional(r[0], r[1], r[2], r[3]));
            clinic.getSchedule().addSlot(p, TimeSlot.of(r[4], r[5], r[6]));
        }

        for (String[] r : PATIENTS) {
            clinic.getPatients().register(r[0], r[1], r[2], r[3], r[4]);
        }

        // booked appointments go through the real booking operations so slots and resources stay consistent
        AppointmentController booking = new AppointmentController(clinic);
        for (String[] r : BOOKED) {
            booking.startBooking(Appointment.Channel.PHONE);
            booking.registerPatient(r[3], r[4], r[5], r[6], null);
            int slotId = clinic.getSchedule().getAllSlots().stream()
                    .filter(s -> s.getProfessional().hasName(r[0])
                            && s.getTime().date().toString().equals(r[1])
                            && s.getTime().start().toString().equals(r[2]))
                    .findFirst().orElseThrow().getId();
            booking.selectSlot(slotId, r[7]);
            booking.confirmBooking();
        }
        return clinic;
    }
}
