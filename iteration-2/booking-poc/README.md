# Clinic PoC – Book Appointment (Iteration II)

Books an appointment for a patient who has arrived at the clinic (walk-in) or who calls (phone).
Medical services that need extra resources (room, equipment, nurse) reserve them when the appointment is booked.

## Run
```bash
mvn test                                      # JUnit 5 tests
mvn -q compile exec:java                      # interactive clerk menu
mvn -q compile exec:java -Dexec.args=--demo   # scripted walkthrough
```
Without Maven: `javac -d out $(find src/main -name "*.java") && java -cp out clinic.ui.ConsoleApp --demo`

## Booking flow (system operations, `AppointmentController`)
1. `startBooking(channel)`: WALK_IN or PHONE
2. `identifyPatient(ramqOrPhone)`; if the patient isn't found, `registerPatient(first, last, phone, ramq?, insurance?)`
3. find a slot with `AvailabilitySearchController.searchAvailability(criteria)` (specialty, professional, date range, service)
4. `selectSlot(slotId, serviceName)`: returns the resources that will be reserved
5. `confirmBooking()`

Rules: only OPEN slots can be booked; a patient can't have two appointments at the same time;
all of a service's resources must be free (a role like Nurse has capacity = number on shift).
RAMQ and insurance are optional; patients without RAMQ are identified by phone.

## Integration
- Availability slots come from `clinic.getSchedule().addSlot(professional, TimeSlot)`. That's the hook for Make Availability / Import from CSV.
- `ui/SampleData` loads the spec's sample CSV rows (plus a few marked *added*) so the PoC runs alone.
  Replace it with the team's CSV importer when merging.
