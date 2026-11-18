package clinic.domain;

import java.time.LocalDate;
import java.time.LocalTime;

/** Value object: a date with a start and end time. */
public record TimeSlot(LocalDate date, LocalTime start, LocalTime end) {

    public TimeSlot {
        if (date == null || start == null || end == null) {
            throw new IllegalArgumentException("date, start and end are required");
        }
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("end must be after start");
        }
    }

    public static TimeSlot of(String date, String start, String end) {
        return new TimeSlot(LocalDate.parse(date), LocalTime.parse(start), LocalTime.parse(end));
    }

    /** True if both slots are on the same day and their time ranges intersect. */
    public boolean overlaps(TimeSlot other) {
        return date.equals(other.date) && start.isBefore(other.end) && other.start.isBefore(end);
    }

    public boolean isWithin(LocalDate from, LocalDate to) {
        return (from == null || !date.isBefore(from)) && (to == null || !date.isAfter(to));
    }

    @Override
    public String toString() {
        return date + " " + start + "-" + end;
    }
}
