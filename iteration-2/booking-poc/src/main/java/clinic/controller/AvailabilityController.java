package clinic.controller;

import clinic.domain.*;
import clinic.io.AvailabilityCsv;

import java.io.IOException;
import java.nio.file.Path;
import java.time.*;
import java.util.*;
import java.util.function.Predicate;

public class AvailabilityController {
    private final Clinic clinic;

    public AvailabilityController(Clinic clinic) {
        this.clinic = clinic;
    }

    public List<Professional> searchProfessional(String criteria) {
        String key = criteria.trim().toLowerCase(Locale.ROOT);

        return clinic.getProfessionals().stream()
                .filter(p -> p.getId().equalsIgnoreCase(key)
                        || p.getFullName().toLowerCase(Locale.ROOT).contains(key)
                        || p.getSpecialty().toLowerCase(Locale.ROOT).contains(key))
                .toList();
    }

    public List<AvailabilitySlot> makeAvailability(
            String professionalId, String locationId, LocalDate fromDate,
            LocalDate toDate, String repetitionPattern, LocalTime startTime,
            LocalTime endTime, int slotDuration) {

        if (!"MAIN".equalsIgnoreCase(locationId)) {
            throw new BookingException("This prototype uses one location: MAIN");
        }

        Professional professional = clinic.getProfessionals().stream()
                .filter(p -> p.getId().equalsIgnoreCase(professionalId))
                .findFirst()
                .orElseThrow(() -> new BookingException("Professional not found"));

        if (fromDate == null || toDate == null || toDate.isBefore(fromDate)) {
            throw new BookingException("Invalid date range");
        }

        if (startTime == null || endTime == null
                || !endTime.isAfter(startTime)
                || startTime.getSecond() != 0 || endTime.getSecond() != 0
                || startTime.getNano() != 0 || endTime.getNano() != 0) {
            throw new BookingException(
                    "Use start/end times in HH:mm, with end after start");
        }

        long minutes = Duration.between(startTime, endTime).toMinutes();

        if (slotDuration <= 0 || minutes % slotDuration != 0) {
            throw new BookingException(
                    "Slot duration must divide the time interval exactly");
        }

        Predicate<LocalDate> matches = pattern(repetitionPattern);
        List<TimeSlot> times = new ArrayList<>();

        for (LocalDate date = fromDate; ; date = date.plusDays(1)) {
            if (matches.test(date)) {
                for (long offset = 0; offset < minutes; offset += slotDuration) {
                    times.add(new TimeSlot(
                            date,
                            startTime.plusMinutes(offset),
                            startTime.plusMinutes(offset + slotDuration)));
                }
            }

            if (date.equals(toDate)) {
                break;
            }
        }

        if (times.isEmpty()) {
            throw new BookingException("Pattern matches no dates");
        }

        Set<LocalDate> conflicts = new TreeSet<>();

        for (TimeSlot time : times) {
            for (AvailabilitySlot existing : clinic.getSchedule().getAllSlots()) {
                if (existing.getProfessional() == professional
                        && existing.getTime().overlaps(time)) {
                    conflicts.add(time.date());
                }
            }
        }

        if (!conflicts.isEmpty()) {
            throw new BookingException(
                    "Overlapping availability on " + conflicts
                            + "; no slots created");
        }

        List<AvailabilitySlot> created = new ArrayList<>();

        for (TimeSlot time : times) {
            created.add(clinic.getSchedule().addSlot(professional, time));
        }

        return List.copyOf(created);
    }

    private Predicate<LocalDate> pattern(String value) {
        String text = value.trim().toUpperCase(Locale.ROOT);

        if (text.equals("DAILY")) {
            return date -> true;
        }

        if (text.startsWith("WEEKLY:")) {
            Set<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);

            for (String day : text.substring(7).split(",", -1)) {
                days.add(DayOfWeek.valueOf(day.trim()));
            }

            return date -> days.contains(date.getDayOfWeek());
        }

        if (text.startsWith("MONTHLY:")) {
            int day = Integer.parseInt(text.substring(8).trim());

            if (day < 1 || day > 31) {
                throw new BookingException("Monthly day must be 1-31");
            }

            return date -> date.getDayOfMonth() == day;
        }

        throw new BookingException(
                "Use DAILY, WEEKLY:MONDAY,WEDNESDAY or MONTHLY:15");
    }

    public int importAvailability(String sourceFile) throws IOException {
        return new AvailabilityCsv(clinic)
                .importAvailability(Path.of(sourceFile));
    }

    public int exportAvailability(String destinationFile) throws IOException {
        return new AvailabilityCsv(clinic)
                .exportAvailability(Path.of(destinationFile));
    }
}