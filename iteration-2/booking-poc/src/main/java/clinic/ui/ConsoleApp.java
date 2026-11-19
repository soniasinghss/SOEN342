package clinic.ui;

import clinic.controller.AppointmentController;
import clinic.controller.AvailabilityController;
import clinic.controller.AvailabilitySearchController;
import clinic.domain.Appointment;
import clinic.domain.AvailabilitySlot;
import clinic.domain.BookingException;
import clinic.domain.Clinic;
import clinic.domain.MedicalService;
import clinic.domain.Patient;
import clinic.domain.Resource;
import clinic.domain.ResourcePool;
import clinic.domain.ResourceRequirement;
import clinic.domain.SearchCriteria;

import java.io.PrintStream;
import java.time.LocalDate;
import java.time.LocalTime;
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
    private final AvailabilityController availability;
    private final Scanner in;
    private final PrintStream out;

    public ConsoleApp(Clinic clinic, Scanner in, PrintStream out) {
        this.clinic = clinic;
        this.booking = new AppointmentController(clinic);
        this.search = new AvailabilitySearchController(clinic);
        this.availability = new AvailabilityController(clinic);
        this.in = in;
        this.out = out;
    }

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("--demo")) {
            Demo.run(SampleData.load(), System.out);
            return;
        }

        new ConsoleApp(
                emptyClinic(), new Scanner(System.in), System.out).run();
    }

    public void run() {
        out.println("=== Clinic PoC ===");

        while (true) {
            out.println();
            out.println("1) Search availability   2) Book appointment");
            out.println("3) List appointments     4) Make availability");
            out.println("5) Import availability CSV   6) Export availability CSV");
            out.println("0) Exit");

            String choice = ask("> ");

            try {
                switch (choice) {
                    case "1" -> searchFlow();
                    case "2" -> bookFlow();
                    case "3" ->
                            clinic.getAppointments().getAll().forEach(out::println);

                    case "4" -> makeAvailabilityFlow();

                    case "5" -> out.println(
                            "Imported " + availability.importAvailability(
                                    ask("Source CSV path: ")) + " slots.");

                    case "6" -> out.println(
                            "Exported " + availability.exportAvailability(
                                    ask("Destination CSV path: ")) + " slots.");

                    case "0" -> {
                        return;
                    }

                    default -> out.println("Unknown option");
                }

            } catch (BookingException | IllegalArgumentException
                     | java.time.DateTimeException | java.io.IOException e) {
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

        out.println("Services: " + clinic.getServices().stream()
                .map(MedicalService::getName).toList());

        String service =
                blankToNull(ask("Service (filters on resources): "));

        SearchCriteria criteria = new SearchCriteria(
                specialty,
                professional,
                from == null ? null : LocalDate.parse(from),
                to == null ? null : LocalDate.parse(to),
                service);

        List<AvailabilitySlot> slots =
                search.searchAvailability(criteria);

        if (slots.isEmpty()) {
            out.println("No availability matches.");
        }

        slots.forEach(s -> out.println("  " + s));
        return slots;
    }

    private void bookFlow() {
        String channel =
                ask("Walk-in (W) or phone (P)? ").trim().toUpperCase();

        booking.startBooking(channel.startsWith("P")
                ? Appointment.Channel.PHONE
                : Appointment.Channel.WALK_IN);

        String key = ask("Patient RAMQ no. or telephone: ");
        Optional<Patient> found = booking.identifyPatient(key);

        if (found.isPresent()) {
            out.println("Patient: " + found.get());
        } else {
            out.println("New patient.");

            Patient patient = booking.registerPatient(
                    ask("First name: "),
                    ask("Last name: "),
                    ask("Telephone: "),
                    blankToNull(ask("RAMQ no. (empty if none): ")),
                    blankToNull(ask("Insurance (empty if none): ")));

            out.println("Registered " + patient);
        }

        searchFlow();

        int slotId =
                Integer.parseInt(ask("Slot # to book: ").trim());

        String service = ask("Service [Consultation]: ");

        List<ResourceRequirement> requirements = booking.selectSlot(
                slotId, service.isBlank() ? "Consultation" : service);

        out.println(requirements.isEmpty()
                ? "No extra resources needed."
                : "Will reserve: " + requirements);

        if (ask("Confirm booking? (y/n) ").trim().equalsIgnoreCase("y")) {
            out.println("Booked: " + booking.confirmBooking());
        } else {
            out.println("Booking abandoned.");
        }
    }

    private static Clinic emptyClinic() {
        Clinic clinic = new Clinic();
        ResourcePool pool = clinic.getResources();

        Resource room =
                pool.addResource("Procedure Room", Resource.Kind.ROOM, 1);
        Resource cast =
                pool.addResource("Cast Room", Resource.Kind.ROOM, 1);
        Resource ecg =
                pool.addResource("ECG Machine", Resource.Kind.EQUIPMENT, 1);
        Resource nurse =
                pool.addResource("Nurse", Resource.Kind.ROLE, 2);
        Resource tech =
                pool.addResource("Orthopedic Technician", Resource.Kind.ROLE, 1);

        clinic.addService("Consultation", List.of());

        clinic.addService("ECG", List.of(
                new ResourceRequirement(ecg, 1),
                new ResourceRequirement(nurse, 1)));

        clinic.addService("Skin Biopsy", List.of(
                new ResourceRequirement(room, 1),
                new ResourceRequirement(nurse, 1)));

        clinic.addService("Cast Application", List.of(
                new ResourceRequirement(cast, 1),
                new ResourceRequirement(tech, 1)));

        return clinic;
    }

    private void makeAvailabilityFlow() {
        List<clinic.domain.Professional> professionals =
                availability.searchProfessional(
                        ask("Professional name, specialty or ID (empty = all): "));

        professionals.forEach(p ->
                out.println(p.getId() + ": " + p));

        if (professionals.isEmpty()) {
            out.println("No professionals. Import a CSV first.");
            return;
        }

        String id = ask("Professional ID: ");
        LocalDate from =
                LocalDate.parse(ask("From date (yyyy-mm-dd): "));
        LocalDate to =
                LocalDate.parse(ask("To date (yyyy-mm-dd): "));

        String pattern = ask(
                "Pattern (DAILY / WEEKLY:MONDAY,WEDNESDAY / MONTHLY:15): ");

        LocalTime start = LocalTime.parse(ask("Start (HH:mm): "));
        LocalTime end = LocalTime.parse(ask("End (HH:mm): "));

        int minutes =
                Integer.parseInt(ask("Slot duration in minutes: "));

        List<AvailabilitySlot> created =
                availability.makeAvailability(
                        id, "MAIN", from, to, pattern, start, end, minutes);

        out.println("Created " + created.size() + " slots from "
                + created.get(0).getTime().date() + " to "
                + created.get(created.size() - 1).getTime().date());
    }

    private String ask(String prompt) {
        out.print(prompt);
        return in.hasNextLine() ? in.nextLine() : "0";
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}