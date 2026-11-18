package clinic.ui;

import clinic.controller.AppointmentController;
import clinic.controller.AvailabilitySearchController;
import clinic.domain.Appointment;
import clinic.domain.AvailabilitySlot;
import clinic.domain.BookingException;
import clinic.domain.Clinic;
import clinic.domain.MedicalService;
import clinic.domain.Patient;
import clinic.domain.ResourceRequirement;
import clinic.domain.SearchCriteria;

import java.io.PrintStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

/**
 * Text UI for the booking PoC (the "clerk's screen"). It only calls controller
 * system operations, never the domain directly (except to display results).
 *
 *   mvn -q compile exec:java                       interactive menu
 *   mvn -q compile exec:java -Dexec.args=--demo    scripted walkthrough
 */
public class ConsoleApp {
    private final Clinic clinic;
    private final AppointmentController booking;
    private final AvailabilitySearchController search;
    private final Scanner in;
    private final PrintStream out;

    public ConsoleApp(Clinic clinic, Scanner in, PrintStream out) {
        this.clinic = clinic;
        this.booking = new AppointmentController(clinic);
        this.search = new AvailabilitySearchController(clinic);
        this.in = in;
        this.out = out;
    }

    public static void main(String[] args) {
        Clinic clinic = SampleData.load();
        if (args.length > 0 && args[0].equals("--demo")) {
            Demo.run(clinic, System.out);
            return;
        }
        new ConsoleApp(clinic, new Scanner(System.in), System.out).run();
    }

    public void run() {
        out.println("=== Clinic PoC - Booking & Search Availability (sample data loaded) ===");
        while (true) {
            out.println();
            out.println("1) Search availability   2) Book appointment");
            out.println("3) List appointments     0) Exit");
            String choice = ask("> ");
            try {
                switch (choice) {
                    case "1" -> searchFlow();
                    case "2" -> bookFlow();
                    case "3" -> clinic.getAppointments().getAll().forEach(out::println);
                    case "0" -> { return; }
                    default -> out.println("Unknown option");
                }
            } catch (BookingException | IllegalArgumentException | java.time.DateTimeException e) {
                out.println("! " + e.getMessage());
            }
        }
    }

    private List<AvailabilitySlot> searchFlow() {
        out.println("Leave a field empty to skip it.");
        String specialty = blankToNull(ask("Specialty: "));
        String professional = blankToNull(ask("Professional name: "));
        String from = blankToNull(ask("From date (yyyy-mm-dd): "));
        String to = blankToNull(ask("To date (yyyy-mm-dd): "));
        out.println("Services: " + clinic.getServices().stream().map(MedicalService::getName).toList());
        String service = blankToNull(ask("Service (filters on resources): "));
        SearchCriteria c = new SearchCriteria(specialty, professional,
                from == null ? null : LocalDate.parse(from), to == null ? null : LocalDate.parse(to), service);
        List<AvailabilitySlot> slots = search.searchAvailability(c);
        if (slots.isEmpty()) out.println("No availability matches.");
        slots.forEach(s -> out.println("  " + s));
        return slots;
    }

    private void bookFlow() {
        String ch = ask("Walk-in (W) or phone (P)? ").trim().toUpperCase();
        booking.startBooking(ch.startsWith("P") ? Appointment.Channel.PHONE : Appointment.Channel.WALK_IN);

        String key = ask("Patient RAMQ no. or telephone: ");
        Optional<Patient> found = booking.identifyPatient(key);
        if (found.isPresent()) {
            out.println("Patient: " + found.get());
        } else {
            out.println("New patient.");
            Patient p = booking.registerPatient(ask("First name: "), ask("Last name: "),
                    ask("Telephone: "), blankToNull(ask("RAMQ no. (empty if none): ")),
                    blankToNull(ask("Insurance (empty if none): ")));
            out.println("Registered " + p);
        }

        searchFlow();
        int slotId = Integer.parseInt(ask("Slot # to book: ").trim());
        String service = ask("Service [Consultation]: ");
        List<ResourceRequirement> reqs = booking.selectSlot(slotId, service.isBlank() ? "Consultation" : service);
        out.println(reqs.isEmpty() ? "No extra resources needed." : "Will reserve: " + reqs);

        if (ask("Confirm booking? (y/n) ").trim().equalsIgnoreCase("y")) {
            out.println("Booked: " + booking.confirmBooking());
        } else {
            out.println("Booking abandoned.");
        }
    }

    private String ask(String prompt) {
        out.print(prompt);
        return in.hasNextLine() ? in.nextLine() : "0";
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
