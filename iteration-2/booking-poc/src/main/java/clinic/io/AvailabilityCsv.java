package clinic.io;

import clinic.domain.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class AvailabilityCsv {
    private static final List<String> HEADER = List.of(
            "First Name", "Last Name", "Title", "Specialty",
            "Date", "Start", "End");

    private final Clinic clinic;

    private record Row(
            String first, String last, String title, String specialty,
            TimeSlot time, AvailabilitySlot.Status status) {

        String name() {
            return first + " " + last;
        }

        String key() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public AvailabilityCsv(Clinic clinic) {
        this.clinic = clinic;
    }

    public int importAvailability(Path source) throws IOException {
        String text = Files.readString(source, StandardCharsets.UTF_8);

        if (text.startsWith("\uFEFF")) {
            text = text.substring(1);
        }

        List<List<String>> records = parse(text);

        if (records.isEmpty()) {
            throw new BookingException("CSV is empty");
        }

        List<String> header = records.get(0);

        if ((header.size() != 7 && header.size() != 8)
                || !header.subList(0, 7).equals(HEADER)
                || (header.size() == 8 && !header.get(7).equals("Status"))) {
            throw new BookingException(
                    "Expected CSV header: "
                            + String.join(",", HEADER)
                            + ",Status (optional)");
        }

        List<Row> pending = new ArrayList<>();

        for (int i = 1; i < records.size(); i++) {
            List<String> fields = records.get(i);

            try {
                if (fields.size() != header.size()) {
                    throw new IllegalArgumentException("Wrong column count");
                }

                for (int j = 0; j < 7; j++) {
                    if (fields.get(j).isBlank()) {
                        throw new IllegalArgumentException(
                                "Missing " + HEADER.get(j));
                    }
                }

                Row row = new Row(
                        fields.get(0),
                        fields.get(1),
                        fields.get(2),
                        fields.get(3),
                        TimeSlot.of(
                                fields.get(4), fields.get(5), fields.get(6)),
                        fields.size() == 7
                                ? AvailabilitySlot.Status.OPEN
                                : AvailabilitySlot.Status.valueOf(
                                        fields.get(7).toUpperCase(Locale.ROOT)));

                Professional professional =
                        exactProfessional(row.name()).orElse(null);

                if (professional != null
                        && (!professional.getTitle().equalsIgnoreCase(row.title())
                        || !professional.getSpecialty()
                                .equalsIgnoreCase(row.specialty()))) {
                    throw new IllegalArgumentException(
                            "Professional details conflict with existing data");
                }

                boolean duplicate = false;

                for (AvailabilitySlot slot :
                        clinic.getSchedule().getAllSlots()) {

                    if (slot.getProfessional() != professional
                            || !slot.getTime().overlaps(row.time())) {
                        continue;
                    }

                    if (!slot.getTime().equals(row.time())
                            || slot.getStatus() != row.status()) {
                        throw new IllegalArgumentException(
                                "Overlapping interval or conflicting status");
                    }

                    duplicate = true;
                }

                for (Row earlier : pending) {
                    if (!earlier.key().equals(row.key())) {
                        continue;
                    }

                    if (!earlier.title().equalsIgnoreCase(row.title())
                            || !earlier.specialty()
                                    .equalsIgnoreCase(row.specialty())) {
                        throw new IllegalArgumentException(
                                "Inconsistent professional details in CSV");
                    }

                    if (!earlier.time().overlaps(row.time())) {
                        continue;
                    }

                    if (!earlier.time().equals(row.time())
                            || earlier.status() != row.status()) {
                        throw new IllegalArgumentException(
                                "Overlapping intervals or conflicting status in CSV");
                    }

                    duplicate = true;
                }

                if (!duplicate) {
                    pending.add(row);
                }

            } catch (IllegalArgumentException | java.time.DateTimeException e) {
                throw new BookingException(
                        "CSV record " + (i + 1) + ": " + e.getMessage()
                                + "; nothing imported");
            }
        }

        // Validate the whole file before changing the shared clinic.
        for (Row row : pending) {
            Professional professional =
                    exactProfessional(row.name()).orElseGet(() ->
                            clinic.addProfessional(
                                    row.first(), row.last(),
                                    row.title(), row.specialty()));

            AvailabilitySlot slot =
                    clinic.getSchedule().addSlot(professional, row.time());

            if (row.status() == AvailabilitySlot.Status.BOOKED) {
                slot.book();
            }
        }

        return pending.size();
    }

    public int exportAvailability(Path destination) throws IOException {
        List<AvailabilitySlot> slots =
                clinic.getSchedule().getAllSlots();

        StringBuilder text = new StringBuilder(
                String.join(",", HEADER)).append(",Status\n");

        for (AvailabilitySlot slot : slots) {
            Professional p = slot.getProfessional();
            TimeSlot t = slot.getTime();

            List<String> fields = List.of(
                    p.getFirstName(),
                    p.getLastName(),
                    p.getTitle(),
                    p.getSpecialty(),
                    t.date().toString(),
                    t.start().toString(),
                    t.end().toString(),
                    slot.getStatus().name());

            text.append(fields.stream()
                            .map(AvailabilityCsv::quote)
                            .collect(java.util.stream.Collectors.joining(",")))
                    .append('\n');
        }

        Path target = destination.toAbsolutePath();
        Path temporary = Files.createTempFile(
                target.getParent(), "availability-", ".tmp");

        try {
            Files.writeString(temporary, text, StandardCharsets.UTF_8);
            Files.move(
                    temporary, target, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }

        return slots.size();
    }

    private Optional<Professional> exactProfessional(String name) {
        return clinic.getProfessionals().stream()
                .filter(p -> p.getFullName().equalsIgnoreCase(name))
                .findFirst();
    }

    private static String quote(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private static List<List<String>> parse(String text) {
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder field = new StringBuilder();

        boolean quoted = false;
        boolean closed = false;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (quoted) {
                if (c == '"') {
                    if (i + 1 < text.length()
                            && text.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        quoted = false;
                        closed = true;
                    }
                } else {
                    field.append(c);
                }

            } else if (c == '"') {
                if (field.length() != 0 || closed) {
                    throw new BookingException("Invalid CSV quote");
                }
                quoted = true;

            } else if (c == ',' || c == '\n' || c == '\r') {
                row.add(field.toString().trim());
                field.setLength(0);
                closed = false;

                if (c != ',') {
                    if (!(row.size() == 1 && row.get(0).isEmpty())) {
                        rows.add(row);
                    }

                    row = new ArrayList<>();

                    if (c == '\r' && i + 1 < text.length()
                            && text.charAt(i + 1) == '\n') {
                        i++;
                    }
                }

            } else {
                if (closed && !Character.isWhitespace(c)) {
                    throw new BookingException(
                            "Text after closing CSV quote");
                }

                if (!closed) {
                    field.append(c);
                }
            }
        }

        if (quoted) {
            throw new BookingException("Unclosed CSV quote");
        }

        if (field.length() > 0 || !row.isEmpty() || closed) {
            row.add(field.toString().trim());
            rows.add(row);
        }

        return rows;
    }
}