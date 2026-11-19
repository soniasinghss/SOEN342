# Clinic PoC - Book Appointment (Iteration II)

Books an appointment for a patient who has arrived at the clinic (walk-in) or who calls (phone).
Medical services that need extra resources (room, equipment, nurse) reserve them when the appointment is booked.

Includes recurring availability and CSV import/export.

Requires JDK 17 or newer.

## Run

With Maven:

```bash
mvn test
mvn -q compile exec:java
mvn -q compile exec:java -Dexec.args=--demo
```

Without Maven, open PowerShell in `iteration-2/booking-poc`:

```powershell
$files = Get-ChildItem -Recurse src/main/java -Filter *.java | ForEach-Object { $_.FullName }
javac -encoding UTF-8 -d out $files
java -cp out clinic.ui.ConsoleApp
```

To run the booking demo:

```powershell
java -cp out clinic.ui.ConsoleApp --demo
```

Normal startup has no professionals, availability slots, patients or appointments.
Services and resources are predefined. Import a CSV file to load initial availability.

## Booking flow (system operations, `AppointmentController`)

1. `startBooking(channel)`: WALK_IN or PHONE
2. `identifyPatient(ramqOrPhone)`. If the patient isn't found, use `registerPatient(first, last, phone, ramq?, insurance?)`
3. Find a slot with `AvailabilitySearchController.searchAvailability(criteria)` using specialty, professional, date range or service
4. `selectSlot(slotId, serviceName)`: returns the resources needed
5. `confirmBooking()`

Only OPEN slots can be booked. A patient cannot have overlapping appointments.
All required resources must be available. A role such as Nurse has a capacity equal to the number available.

RAMQ and insurance are optional. Patients without RAMQ are identified by phone.

## Make availability

Choose option 4. Find a professional and enter their ID, date range, repetition pattern,
start/end times and slot duration in minutes.

Supported patterns:

- `DAILY`
- `WEEKLY:MONDAY`
- `WEEKLY:MONDAY,WEDNESDAY`
- `MONTHLY:15`

Example after importing `data/availability.csv`:

```text
Professional ID: P1
From date: 2026-11-01
To date: 2026-11-30
Pattern: WEEKLY:MONDAY
Start: 15:00
End: 18:00
Slot duration: 30
```

This creates 30 slots. Both dates are included.
Monthly patterns skip months that do not contain the selected day.

Slot duration must fit evenly into the time range.
If any interval overlaps existing availability, no slots are created.

The current prototype uses one location, `MAIN`.

## Import and export CSV

Choose option 5 and enter:

```text
data/availability.csv
```

A second sample is provided in `data/additional-availability.csv`.

Files use UTF-8 with this header:

```csv
First Name,Last Name,Title,Specialty,Date,Start,End
```

Dates use `yyyy-MM-dd`. Times use `HH:mm`.
An optional `Status` column accepts `OPEN` or `BOOKED`.
Slots are OPEN when the column is omitted.

Exact duplicates are skipped. Invalid data, conflicting professional details,
overlapping intervals or conflicting statuses reject the import without changing existing data.

Choose option 6 and enter an output path, for example:

```text
data/exported-availability.csv
```

The folder must exist. An existing output file is replaced.
All slots are exported with their status, including BOOKED slots.

Exported files can be imported again. They contain availability only.
Patient records, appointment details and resource allocations are not exported.

## Integration

- Slots are added through `clinic.getSchedule().addSlot(professional, TimeSlot)`.
- `AvailabilityController` handles recurring availability and CSV operations.
- `AvailabilityCsv` reads and writes CSV files.
- The controllers share one `Clinic` instance. Search and booking use the same slots created or imported through the menu.
- `ui/SampleData` is used by `--demo` and the existing booking tests.
- Data stays in memory while the application runs. There is no database or automatic saving.