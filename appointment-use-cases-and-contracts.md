# Appointment Tracking System
## Iteration II - Use Cases and Operation Contracts

### Changes since Iteration I

- **Critical set reduced to scheduling.** Iteration I marked 11 use cases as critical but provided SSDs for only 3. The critical set is now the scheduling use cases listed below, and every one of them has an SSD. Billing, clinical, referral and authentication use cases are non-critical.
- **New requirements analysed:** waiting lists (UC8 to UC11) and resource allocation (impact on UC1, UC2, UC3 and their contracts).
- **New use cases from the Proof of Concept scope:** Make Availability (UC6), Search Availability (UC7), Import Availability from CSV and Export Availability to CSV (non-critical).
- **UC1 aligned with the Proof of Concept:** Book Appointment now uses the system operations implemented in the code: startBooking, identifyPatient, registerPatient, searchAvailability, selectSlot and confirmBooking. They replace searchPatient and bookAppointment for this use case.
- **Renamed operation:** findAvailableSlots(serviceCode, professionalId, locationId, date) is now searchAvailability(criteria).
- **Domain model:** added Waitlist, WaitlistEntry, Resource, ResourceRequirement and ResourceAllocation.

Diagrams: [use case diagram](use_case_diagram.png), [domain model](domain_model.png).

### Critical use cases

| # | Critical use case | System operations | SSD |
| --- | --- | --- | --- |
| UC1 | Book Appointment | startBooking, identifyPatient, registerPatient, searchAvailability, selectSlot, confirmBooking | [ssd_uc01_book_appointment.png](ssd/ssd_uc01_book_appointment.png) |
| UC2 | Reschedule Appointment | getAppointment, searchAvailability, rescheduleAppointment | [ssd_uc02_reschedule_appointment.png](ssd/ssd_uc02_reschedule_appointment.png) |
| UC3 | Cancel Appointment | getAppointment, cancelAppointment | [ssd_uc03_cancel_appointment.png](ssd/ssd_uc03_cancel_appointment.png) |
| UC4 | Register Patient | searchPatient, registerPatient | [ssd_uc04_register_patient.png](ssd/ssd_uc04_register_patient.png) |
| UC5 | Register Patient Arrival | searchPatient, findAppointments, registerArrival | [ssd_uc05_register_patient_arrival.png](ssd/ssd_uc05_register_patient_arrival.png) |
| UC6 | Make Availability | searchProfessional, makeAvailability | [ssd_uc06_make_availability.png](ssd/ssd_uc06_make_availability.png) |
| UC7 | Search Availability | searchAvailability | [ssd_uc07_search_availability.png](ssd/ssd_uc07_search_availability.png) |
| UC8 | Add Patient to Waitlist | searchPatient, addToWaitlist | [ssd_uc08_add_patient_to_waitlist.png](ssd/ssd_uc08_add_patient_to_waitlist.png) |
| UC9 | Offer Slot to Waitlisted Patient | getEligibleWaitlistEntries, offerSlot, acceptOffer, declineOffer | [ssd_uc09_offer_slot_to_waitlisted_patient.png](ssd/ssd_uc09_offer_slot_to_waitlisted_patient.png) |
| UC10 | Change Waitlist Priority | getWaitlist, changeWaitlistPriority | [ssd_uc10_change_waitlist_priority.png](ssd/ssd_uc10_change_waitlist_priority.png) |
| UC11 | Remove Patient from Waitlist | getWaitlist, removeFromWaitlist | [ssd_uc11_remove_patient_from_waitlist.png](ssd/ssd_uc11_remove_patient_from_waitlist.png) |

UC8 to UC11 together cover the Waitlist requirement. Each SSD shows the main success scenario and one alternative or failure scenario.

&nbsp;

### UC1: Book Appointment

**Primary Actor:** Administrative Staff.

**Stakeholders and Interests:**
- **Patient:** wants an appointment for the required service at a suitable time.
- **Administrative Staff:** wants to book for the correct patient without errors.
- **Healthcare Professional:** wants appointments to respect their availability.
- **Clinic:** wants to prevent conflicting bookings and double-booked resources.

**Preconditions:** The actor is authenticated and authorized to book appointments. Professionals, services, resources and availability are recorded in the system.

**Success Guarantee:** A booked appointment is recorded for the patient, the selected availability slot and the medical service. The slot is no longer open and the resources the service requires are reserved for the appointment's time.

**Main Success Scenario:**
1. Staff start a booking and indicate whether the patient has arrived at the clinic (walk-in) or is calling (phone).
2. The system opens a pending booking.
3. Staff identify the patient by RAMQ number or telephone number.
4. The system displays the patient and attaches the patient to the booking.
5. Staff search availability by specialty, professional, date range and/or medical service (Search Availability, UC7).
6. The system displays the open slots.
7. Staff select a slot and the medical service.
8. The system checks that the slot is still open, that the patient has no other appointment at that time and that the resources the service requires are free. It displays the resources that will be reserved.
9. Staff confirm the booking.
10. The system reserves the resources, books the slot and records the appointment.
11. The system displays the appointment identifier and confirmation details.

**Extensions:**
- **4a. Patient Not Found:** Staff register the patient with first name, last name, telephone and, when available, RAMQ number and insurance (Register Patient, UC4). The new patient is attached to the booking and the scenario continues at step 5.
- **6a. No Open Slot:** Staff change the criteria, add the patient to the waitlist (UC8), or end the request without booking.
- **8a. Slot No Longer Available (Failure):** The system rejects the selection. Staff select another slot or end the request.
- **8b. Patient Already Has an Appointment at That Time (Failure):** The system rejects the selection. Staff select another slot or end the request.
- **8c. Required Resource Not Available (Failure):** A room, equipment or role the service needs is fully allocated at that time. The system rejects the selection. Staff select another slot or end the request.
- **10a. Slot Booked by Someone Else Before Confirmation (Failure):** The system rejects the confirmation and releases the resources it had reserved. No appointment is recorded.

**Special Requirements:** Concurrent requests must not produce conflicting bookings or over-allocated resources.

**Technology and Data Variations:** Patients without a RAMQ number are identified by telephone number. A role resource such as Nurse has a capacity equal to the number on shift. Interaction is through the CLI.

**Open Issues:** Referral verification and clinic location are not part of the Iteration II Proof of Concept and are deferred to Iteration III.

&nbsp;

### UC2: Reschedule Appointment

**Primary Actor:** Administrative Staff. A patient may also reschedule their own appointment when authorized.

**Stakeholders and Interests:**
- **Patient:** wants a new time while retaining the original booking if the change fails.
- **Administrative Staff:** wants to change the correct appointment.
- **Healthcare Professional:** wants an accurate schedule without conflicting bookings.
- **Clinic:** wants to preserve appointment history and record changes.

**Preconditions:** The actor is authenticated and authorized to manage appointments for the patient.

**Success Guarantee:** The existing appointment is moved to the new interval. Its identifier, patient, professional, location, service and referral are retained. The old interval is released, the new interval is reserved and the change is logged.

**Main Success Scenario:**
1. Staff enter the appointment identifier.
2. The system displays the appointment details. Staff verify the patient and appointment.
3. Staff request availability for another date with the same professional, location and service.
4. The system displays suitable available intervals.
5. Staff select a new interval and request the change.
6. The system checks that the appointment is eligible, the new interval is available and scheduling and referral requirements are satisfied.
7. The system updates the appointment, releases the old interval, reserves the new one, moves its resource allocations to the new interval and logs the change.
8. The system displays the updated appointment confirmation.

**Extensions:**
- **2a. Appointment Not Found:** The system reports the error. Staff correct the identifier or end the request.
- **4a. No Available Interval:** Staff request another date or end the request. The original booking is retained.
- **6a. New Interval No Longer Available (Failure):** The system rejects the change. The original appointment and reserved interval remain unchanged.
- **6b. Appointment Not Eligible:** The system rejects the change if the appointment is no longer future and booked, or arrival has been registered.
- **6c. Referral or Scheduling Requirement Not Satisfied:** The system rejects the change and explains the reason. The original booking is retained.
- **5a. Patient Decides to Keep the Original Time:** Staff do not submit the change. The appointment remains unchanged.

**Special Requirements:** The original interval is released only if the new booking succeeds. The old and new appointment times are recorded in the audit trail.

**Technology and Data Variations:** The appointment is retrieved by its unique identifier. Rescheduling changes its time, retaining the same professional, location and service.

**Open Issues:** Any advance notice restriction, whether changing the professional or location should be supported.

&nbsp;

### UC3: Cancel Appointment

**Primary Actor:** Administrative Staff. A patient may also cancel their own appointment when authorized.

**Stakeholders and Interests:**
- Patient: wants the intended appointment cancelled and confirmation provided.
- Administrative Staff: wants to avoid cancelling the wrong appointment.
- Healthcare Professional: wants the cancelled interval released.
- Clinic: wants to retain appointment history and record the cancellation.

**Preconditions:** The actor is authenticated and authorized to manage appointments for the patient.

**Success Guarantee:** The appointment is marked cancelled and retained in the system. Its interval is released and the cancellation is logged.

**Main Success Scenario:**
1. Staff enter the appointment identifier.
2. The system displays the appointment details. Staff verify the patient and appointment.
3. Staff confirm the patient's decision and submit the cancellation request.
4. The system checks that the appointment is eligible for cancellation.
5. The system marks the appointment cancelled, releases its interval and allocated resources and logs the cancellation.
6. The system displays cancellation confirmation, with the eligible waitlist entries for the released interval, if any, ordered by priority.

**Extensions:**
- **2a. Appointment Not Found (Failure):** The system reports the error. Staff correct the identifier or end the request. Nothing is cancelled.
- **3a. Patient Decides to Keep the Appointment:** Staff do not submit cancellation. The booking remains unchanged.
- **4a. Appointment Already Cancelled:** The system reports its current status without changing availability again.
- **6a. Eligible Waitlisted Patients Exist:** Staff may offer the released interval to a waitlisted patient (UC9).
- **4b. Appointment Not Eligible (Failure):** The system rejects cancellation if the appointment is no longer future and booked, or arrival has been registered. Its state remains unchanged.

**Special Requirements:** Cancellation retains appointment history and associated clinical and billing information. Only authorized users may cancel appointments.

**Technology and Data Variations:** The appointment is retrieved by its unique identifier. Confirmation is displayed through the CLI.

**Open Issues:** Cancellation deadlines, fees and whether a cancellation reason is required.

&nbsp;

### UC4: Register Patient

**Primary Actor:** Administrative Staff.

**Preconditions:** The actor is authenticated and authorized to register patients.

**Success Guarantee:** A new patient is recorded with identifying and contact information, a clinical record is opened, insurance coverage is recorded when provided, and the registration is logged.

**Main Success Scenario:**
1. Staff search for the patient to make sure they are not already registered.
2. The system reports no matching patient.
3. Staff enter the patient's first name, last name, telephone and, when available, RAMQ number and insurance.
4. The system validates the information, records the patient, opens a clinical record and logs the registration.
5. The system displays the patient identifier and confirmation.

**Extensions:**
- **2a. Patient Already Registered:** The system displays the match. Staff use the existing patient and end the request.
- **3a. No RAMQ Number:** Staff register the patient without it. The patient is identified by telephone number.
- **4a. Patient Already Registered (Failure):** The RAMQ number belongs to an existing patient. The system rejects the registration and returns the existing patient identifier. Nothing is created.
- **4b. Missing or Invalid Information (Failure):** The system identifies the fields to correct. Nothing is created.

&nbsp;

### UC5: Register Patient Arrival

**Primary Actor:** Administrative Staff.

**Preconditions:** The actor is authenticated and authorized to manage appointments.

**Success Guarantee:** The appointment is marked arrived, its arrival time is recorded and the arrival is logged.

**Main Success Scenario:**
1. The patient arrives at the clinic. Staff search for the patient.
2. The system displays matching patients. Staff identify the correct patient.
3. Staff request the patient's appointments for today.
4. The system displays the appointments with their time, professional and status.
5. Staff select the appointment and register the arrival.
6. The system checks that the appointment is booked and scheduled for today, records the arrival time, sets the status to arrived and logs the arrival.
7. The system displays the arrival confirmation.

**Extensions:**
- **2a. Patient Not Found:** Staff correct the criteria or register the patient (UC4).
- **4a. No Appointment Today:** Staff may book an appointment for the walk-in patient (UC1) or end the request.
- **6a. Appointment Not Eligible (Failure):** The appointment is cancelled, is not today or already has an arrival. The system rejects the request and its state is unchanged.

&nbsp;

### UC6: Make Availability

**Primary Actor:** Administrative Staff.

**Preconditions:** The actor is authenticated and authorized to manage schedules. The professional and location are recorded in the system.

**Success Guarantee:** Availability slots are created for the selected professional over the date range, following the repetition pattern, and can be found by Search Availability.

**Main Success Scenario:**
1. Staff search for the healthcare professional.
2. The system displays matching professionals. Staff select one.
3. Staff enter the location, date range, repetition pattern (for example every Monday, or monthly), start time, end time and slot duration.
4. The system generates the dates matching the pattern, checks them against the professional's existing availability, creates one available slot per interval and logs the change.
5. The system displays the number of slots created and the first and last dates.

**Extensions:**
- **2a. Professional Not Found:** Staff correct the criteria or end the request.
- **4a. Overlaps Existing Availability (Failure):** The system lists the conflicting dates. No slot is created. Staff adjust the request or end it.
- **4b. Invalid Range or Pattern (Failure):** The end is before the start, or the pattern matches no date. No slot is created.

Example: during November, Dr. X is available every Monday from 9:00 to 12:00.

&nbsp;

### UC7: Search Availability

**Primary Actor:** Administrative Staff. Also included by Book Appointment (UC1) and Reschedule Appointment (UC2).

**Preconditions:** The actor is authenticated and authorized to view availability.

**Success Guarantee:** The available intervals matching the criteria are displayed. Nothing is reserved or changed.

**Main Success Scenario:**
1. Staff enter search criteria: any combination of service, professional, specialty, location and date range.
2. The system displays the matching available intervals with professional, specialty, location, date, start and end.

**Extensions:**
- **2a. No Availability Found:** The system returns an empty result. Staff refine the criteria or add the patient to the waitlist (UC8).

&nbsp;

### UC8: Add Patient to Waitlist

**Primary Actor:** Administrative Staff, on the patient's request.

**Preconditions:** The actor is authenticated and authorized to manage the waitlist. The requested professional has no suitable available slot.

**Success Guarantee:** A waitlist entry is recorded for the patient with the requested professional, specialty and/or date range, a priority and status waiting. The request is logged.

**Main Success Scenario:**
1. Staff search for the patient.
2. The system displays matching patients. Staff identify the correct patient.
3. Staff enter the request: professional, specialty and/or date range, the referral type and a description.
4. The system records the entry at the end of the waitlist (lowest priority among waiting entries) and logs it.
5. The system displays the entry identifier and its priority.

**Extensions:**
- **2a. Patient Not Found:** Staff register the patient (UC4) before continuing.
- **4a. Duplicate Request (Failure):** The patient already has an active entry for the same professional or specialty. Nothing is created.
- **4b. Empty Request (Failure):** No professional, specialty or date range was given. Nothing is created.

&nbsp;

### UC9: Offer Slot to Waitlisted Patient

**Primary Actor:** Administrative Staff. Extends Cancel Appointment (UC3) when a slot is released.

**Preconditions:** The actor is authenticated and authorized to manage the waitlist. An available slot exists, typically after a cancellation.

**Success Guarantee:** The slot is booked for the highest-priority patient who accepts it and that patient's entry is marked fulfilled.

**Main Success Scenario:**
1. Staff request the waitlist entries eligible for the released slot.
2. The system displays the eligible entries ordered by priority, with each patient's phone number. An entry is eligible when it is waiting and its professional, specialty and date range match the slot.
3. Staff select the first entry and record the offer.
4. The system marks the entry offered and holds the slot for this patient.
5. Staff phone the patient, who accepts. Staff record the acceptance with the medical service.
6. The system books the appointment for the offered slot as in UC1 (steps 8 and 10), marks the entry fulfilled and logs the booking.
7. The system displays the appointment confirmation.

**Extensions:**
- **2a. No Eligible Entry:** The slot stays available. The request ends.
- **5a. Patient Declines:** Staff record the refusal. The system withdraws the offer, makes the slot available again and sets the entry back to waiting. Staff may remove the patient from the waitlist (UC11), then offer the slot to the next entry.
- **5b. Patient Unreachable:** Handled as 5a without removal.
- **6a. Booking Rejected (Failure):** A required resource is not available. No appointment is created and the entry returns to waiting.

&nbsp;

### UC10: Change Waitlist Priority

**Primary Actor:** Administrative Staff.

**Preconditions:** The actor is authenticated and authorized to manage the waitlist.

**Success Guarantee:** The entry has its new priority, the waitlist order reflects it and the change is logged.

**Main Success Scenario:**
1. Staff request the waitlist, optionally filtered by professional or specialty.
2. The system displays the entries ordered by priority.
3. Staff select an entry and enter its new priority.
4. The system updates the priority, reorders the waitlist and logs the change.
5. The system displays the updated entry.

**Extensions:**
- **4a. Entry Not Active (Failure):** The entry is fulfilled or removed, or the priority value is invalid. The waitlist is unchanged.

&nbsp;

### UC11: Remove Patient from Waitlist

**Primary Actor:** Administrative Staff. Extends Offer Slot to Waitlisted Patient (UC9) when the patient declines.

**Preconditions:** The actor is authenticated and authorized to manage the waitlist.

**Success Guarantee:** The entry is marked removed and no longer eligible for offers. It is retained for history and the removal is logged.

**Main Success Scenario:**
1. Staff request the waitlist and select the entry.
2. The system displays the entries ordered by priority.
3. Staff request the removal and give a reason (declined an offer, no longer needed, booked elsewhere).
4. The system marks the entry removed, releases any slot held for it and logs the removal.
5. The system displays the removal confirmation.

**Extensions:**
- **4a. Entry Not Active (Failure):** The entry is already fulfilled or removed. The waitlist is unchanged.

&nbsp;

### Non-critical use cases added in Iteration II (brief)

- **Import Availability from CSV:** staff give a source file; the system creates the professionals and availability slots it lists. The system starts empty.
- **Export Availability to CSV:** the system writes the entire availability database to a CSV file.
- **Allocate Required Resources:** included by booking; reserves the room, equipment or role quantities a service requires for the appointment's interval.
- **Manage Resources:** the system administrator maintains the predefined list of resources (rooms, equipment, roles and their capacity).

&nbsp;

## System Operations

| Operation | Use case | Contract |
| --- | --- | --- |
| searchPatient(criteria) | UC4, UC5, UC8 | CO1 |
| searchAvailability(criteria) | UC1, UC2, UC7 | CO2 |
| startBooking(channel) | UC1 | CO3.1 |
| identifyPatient(ramqOrPhone) | UC1 | CO3.2 |
| selectSlot(slotId, serviceName) | UC1 | CO3.3 |
| confirmBooking() | UC1 | CO3.4 |
| getAppointment(appointmentId) | UC2, UC3 | CO4 |
| rescheduleAppointment(appointmentId, newStartTime) | UC2 | CO5 |
| cancelAppointment(appointmentId) | UC3 | CO6 |
| registerPatient(firstName, lastName, telephone, ramqNo, insurance) | UC1, UC4 | CO7 |
| findAppointments(patientId, date) | UC5 | CO8 |
| registerArrival(appointmentId) | UC5 | CO9 |
| searchProfessional(criteria) | UC6 | CO10 |
| makeAvailability(professionalId, locationId, fromDate, toDate, repetitionPattern, startTime, endTime, slotDuration) | UC6 | CO11 |
| addToWaitlist(patientId, professionalId, specialtyCode, fromDate, toDate, referralType, description) | UC8 | CO12 |
| getEligibleWaitlistEntries(slotId) | UC9 | CO13 |
| offerSlot(entryId, slotId) | UC9 | CO14 |
| acceptOffer(entryId, serviceName) | UC9 | CO15 |
| declineOffer(entryId) | UC9 | CO16 |
| getWaitlist(criteria) | UC10, UC11 | CO17 |
| changeWaitlistPriority(entryId, newPriority) | UC10 | CO18 |
| removeFromWaitlist(entryId, reason) | UC9, UC11 | CO19 |

The acting UserAccount is identified by the authenticated session. `slotId` denotes one ScheduleSlot (professional, location, start date/time). `ramqNo` and `insurance` are optional. The operation names and parameters of UC1 and UC7 are those of the Proof of Concept controllers (AppointmentController, AvailabilitySearchController). `criteria` for searchAvailability may combine specialty, professional, a date range and a medical service.

&nbsp;


## Operation Contracts

A booking in progress has status `pending` and a successful booking has status `booked` (called confirmed in Iteration I), as in the Proof of Concept. Rescheduling and cancellation apply to future, booked appointments before arrival. These are initial assumptions pending clarification of the open issues above. A ScheduleSlot is identified by its professional, location and start date/time. Its duration is determined from its start and end times.

&nbsp;

### CO1: searchPatient

**Operation:** searchPatient(criteria)

**Cross References:** UC4: Register Patient, UC5: Register Patient Arrival, UC8: Add Patient to Waitlist. (UC1 uses identifyPatient, CO3.2.)

**Preconditions:** The actor is authenticated and authorized to search patient information.

**Postconditions:**
- Patient information was unchanged.
- For each patient whose identifying information was displayed, an AuditEntry instance was created, its timestamp, action and details were set, and it was associated with the acting UserAccount and that Patient.

**Result:** Matching patient summaries, including patient identifiers, or an empty result. Criteria may contain insurance number, date of birth, phone number or partial names.

&nbsp;


### CO2: searchAvailability

**Operation:** searchAvailability(criteria)

Renamed from findAvailableSlots(serviceCode, professionalId, locationId, date) in Iteration I. Search Availability is now a use case of its own (UC7) and the criteria also accept a specialty and a date range.

**Cross References:** UC1: Book Appointment, UC2: Reschedule Appointment, UC7: Search Availability.

**Preconditions:** The actor is authorized to view availability. Any MedicalService named in the criteria exists.

**Postconditions:** No instances, attributes or associations were changed. No interval was reserved.

**Result:** Suitable available intervals, or an empty result. Availability considers schedules, service duration, location, existing active appointments and, when a service is given, the free capacity of the Resources it requires. Returned availability does not guarantee that a later booking will succeed.

&nbsp;


### CO3.1: startBooking

**Operation:** startBooking(channel)

**Cross References:** UC1: Book Appointment.

**Preconditions:** The actor is authenticated and authorized to book appointments. channel is walk-in or phone.

**Postconditions:**
- An Appointment instance a was created. (instance creation)
- a.appointmentId was assigned a unique value, a.channel was set to channel and a.status was set to pending. (attribute modification)
- a became the booking in progress for the session.

&nbsp;

### CO3.2: identifyPatient

**Operation:** identifyPatient(ramqOrPhone)

**Cross References:** UC1: Book Appointment.

**Preconditions:** A booking is in progress (startBooking).

**Postconditions On Success:**
- The pending Appointment a was associated with the Patient whose RAMQ number or telephone number equals ramqOrPhone. (association formed)

**Result:** The patient's details, or an empty result when no patient matches. With an empty result nothing is changed and staff register the patient (registerPatient, CO7).

&nbsp;

### CO3.3: selectSlot

**Operation:** selectSlot(slotId, serviceName)

**Cross References:** UC1: Book Appointment.

**Preconditions:** A booking is in progress and a Patient is associated with it. The ScheduleSlot and the MedicalService exist.

**Postconditions On Success:**
- The pending Appointment a was associated with the ScheduleSlot and the MedicalService. (associations formed)
- a.dateTime and a.duration were set from the slot. (attribute modification)
- Nothing was reserved yet: the slot's isAvailable and all ResourceAllocations were unchanged.

**Result:** The ResourceRequirements of the service, that is the resources that will be reserved on confirmation.

**Failure:** If the slot is no longer available, the patient already has a booked appointment overlapping the slot's time, or a required Resource has no free capacity at that time, nothing is changed and the reason is returned.

&nbsp;

### CO3.4: confirmBooking

**Operation:** confirmBooking()

**Cross References:** UC1: Book Appointment.

**Preconditions:** A booking is in progress with a Patient, a ScheduleSlot and a MedicalService associated.

**Postconditions On Success:**
- For each ResourceRequirement of the MedicalService, a ResourceAllocation instance ra was created, ra.quantity was set to the required quantity and ra was associated with a and with the required Resource. (instance creation, attribute modification, associations formed)
- The selected ScheduleSlot's isAvailable was set to false. (attribute modification)
- a.status was set to booked. (attribute modification)
- a was recorded with the clinic's appointments and is no longer the booking in progress.
- An AuditEntry instance ae was created, its timestamp, action and details were set to record the booking, and ae was associated with the acting UserAccount and the Patient. (instance creation, attribute modification, associations formed)

**Failure:** If the slot was booked in the meantime or a required Resource no longer has free capacity, no ResourceAllocation is kept, the slot is unchanged and a remains pending. The reason is returned.

&nbsp;


### CO4: getAppointment

**Operation:** getAppointment(appointmentId)

**Cross References:** UC2: Reschedule Appointment, UC3: Cancel Appointment.

**Preconditions:** The actor is authenticated and has appointment-management access. Access to the patient's information is checked before disclosure.

**Postconditions On Authorized Access:**
- The Appointment and its associations were unchanged.
- An AuditEntry instance was created. Its timestamp, action and details were set to record the access and appointment identifier. It was associated with the acting UserAccount and the appointment's Patient.

**Result:** Appointment details, including its status, date/time, professional, location and service.

**Failure:** If the appointment does not exist or access is denied, no details are disclosed and no domain state is changed by this operation.

&nbsp;


### CO5: rescheduleAppointment

**Operation:** rescheduleAppointment(appointmentId, newStartTime)

**Cross References:** UC2: Reschedule Appointment.

**Preconditions:** The Appointment exists and the actor is authorized to manage it.

**Postconditions On Success:**
- The existing Appointment a was retained with its original identifier.
- a.dateTime was set to newStartTime and a.duration was set to the new interval duration. (attribute modification)
- a.status remained booked. Its patient, professional, location, service and referral associations were unchanged.
- The old ScheduleSlot's isAvailable was set to true and the new ScheduleSlot's isAvailable was set to false. (attribute modification)
- The appointment's ResourceAllocations were kept and now apply to the new interval. Each required Resource had free capacity at the new time.
- An AuditEntry instance ae was created. Its timestamp, action and details were set to record the change, appointment identifier and old and new times. ae was associated with the acting UserAccount and Patient. (instance creation, attribute modification, associations formed)

**Failure:** If the appointment is ineligible, the new interval is unavailable, a required Resource has no free capacity at the new time or a scheduling/referral requirement fails, the appointment and slot availability remain unchanged. The rejection reason is returned.

&nbsp;

### CO6: cancelAppointment

**Operation:** cancelAppointment(appointmentId)

**Cross References:** UC3: Cancel Appointment.

**Preconditions:** The Appointment exists and the actor is authorized to manage it.

**Postconditions On Success:**
- The Appointment was retained and its status was set to cancelled. (attribute modification)
- The corresponding ScheduleSlot's isAvailable was set to true. (attribute modification)
- The appointment's ResourceAllocation instances were deleted, freeing the capacity of their Resources. (instance deletion, associations broken)
- No WaitlistEntry was changed. Eligible entries are only reported.
- The appointment's patient, professional, location, service and referral associations were unchanged. No related clinical or billing information was deleted.
- An AuditEntry instance ae was created. Its timestamp, action and details were set to record the cancellation and appointment identifier. ae was associated with the acting UserAccount and Patient. (instance creation, attribute modification, associations formed)

**Result:** Cancellation confirmation and the eligible WaitlistEntries for the released ScheduleSlot (same result as getEligibleWaitlistEntries, CO13), or none.

**Failure:** If the appointment is already cancelled or otherwise ineligible, its state and slot availability remain unchanged. The current status or rejection reason is returned.

&nbsp;

## Operation Contracts added in Iteration II

Waitlist entries have status `waiting`, `offered`, `fulfilled` or `removed`. An entry is active when it is waiting or offered. A lower priority number is served first.

&nbsp;

### CO7: registerPatient

**Operation:** registerPatient(firstName, lastName, telephone, ramqNo, insurance)

**Cross References:** UC4: Register Patient, UC1: Book Appointment (extension 4a).

**Preconditions:** The actor is authenticated and authorized to register patients. telephone is supplied. ramqNo and insurance are optional.

**Postconditions On Success:**
- A Patient instance p was created. (instance creation)
- p.patientId was assigned a unique value and p.firstName, p.lastName and p.phoneNumber were set. (attribute modification)
- A ClinicalRecord instance cr was created, cr.recordId was assigned, cr.dateOpened was set to the current date and cr was associated with p. (instance creation, attribute modification, association formed)
- If ramqNo was supplied, an InsuranceCoverage instance was created with insuranceNumber set to ramqNo and coverageType set to RAMQ, and it was associated with p. (instance creation, attribute modification, association formed)
- If insurance was supplied, an InsuranceCoverage instance was created with coverageType set to private and insurerName set to insurance, and it was associated with p. (instance creation, attribute modification, association formed)
- When called during Book Appointment, the pending Appointment was associated with p. (association formed)
- An AuditEntry instance was created, its timestamp, action and details were set, and it was associated with the acting UserAccount and p.

**Failure:** If a Patient with the same RAMQ number exists or the telephone is missing, nothing is created and the reason is returned.

&nbsp;

### CO8: findAppointments

**Operation:** findAppointments(patientId, date)

**Cross References:** UC5: Register Patient Arrival.

**Preconditions:** The Patient exists and the actor has appointment-management access.

**Postconditions:**
- No Appointment was changed.
- An AuditEntry instance was created to record the access. It was associated with the acting UserAccount and the Patient.

**Result:** The patient's appointments on the date with identifier, time, professional and status, or an empty result.

&nbsp;

### CO9: registerArrival

**Operation:** registerArrival(appointmentId)

**Cross References:** UC5: Register Patient Arrival.

**Preconditions:** The Appointment exists and the actor is authorized to manage it.

**Postconditions On Success:**
- a.arrivalTime was set to the current time and a.status was set to arrived. (attribute modification)
- The appointment's associations were unchanged.
- An AuditEntry instance was created, its timestamp, action and details were set to record the arrival, and it was associated with the acting UserAccount and the Patient.

**Failure:** If the appointment is not booked, is not scheduled for the current date or already has an arrival time, its state is unchanged and the reason is returned.

&nbsp;

### CO10: searchProfessional

**Operation:** searchProfessional(criteria)

**Cross References:** UC6: Make Availability.

**Preconditions:** The actor is authenticated and authorized to manage schedules.

**Postconditions:** No instances, attributes or associations were changed.

**Result:** Matching HealthcareProfessionals with identifier, name and specialties, or an empty result. Criteria may contain a partial name or a specialty.

&nbsp;

### CO11: makeAvailability

**Operation:** makeAvailability(professionalId, locationId, fromDate, toDate, repetitionPattern, startTime, endTime, slotDuration)

**Cross References:** UC6: Make Availability.

**Preconditions:** The HealthcareProfessional and ClinicLocation exist and the professional practises at the location. The actor is authorized to manage schedules.

**Postconditions On Success:**
- For each date between fromDate and toDate that matches repetitionPattern, and for each interval of slotDuration between startTime and endTime, a ScheduleSlot instance ss was created. (instance creation)
- ss.date, ss.startTime and ss.endTime were set and ss.isAvailable was set to true. (attribute modification)
- ss was associated with the HealthcareProfessional and the ClinicLocation. (associations formed)
- An AuditEntry instance was created to record the change and was associated with the acting UserAccount.

**Result:** The number of slots created with the first and last dates.

**Failure:** If any generated interval overlaps an existing ScheduleSlot of the professional, the range is invalid or the pattern matches no date, no ScheduleSlot is created. The conflicting dates or the reason are returned.

&nbsp;

### CO12: addToWaitlist

**Operation:** addToWaitlist(patientId, professionalId, specialtyCode, fromDate, toDate, referralType, description)

**Cross References:** UC8: Add Patient to Waitlist.

**Preconditions:** The Patient exists. At least one of professionalId, specialtyCode or the date range is supplied, and any professional or specialty named exists. The actor is authorized to manage the waitlist.

**Postconditions On Success:**
- A WaitlistEntry instance we was created. (instance creation)
- we.entryId was assigned a unique value, we.dateAdded was set to the current date, we.fromDate, we.toDate, we.referralType and we.description were set, we.status was set to waiting and we.priority was set to the lowest priority among waiting entries. (attribute modification)
- we was associated with the Waitlist and the Patient, and with the requested HealthcareProfessional and MedicalSpecialty when supplied. (associations formed)
- An AuditEntry instance was created to record the request and was associated with the acting UserAccount and the Patient.

**Failure:** If the patient already has an active entry for the same professional or specialty, or the request is empty, nothing is created and the reason is returned.

&nbsp;

### CO13: getEligibleWaitlistEntries

**Operation:** getEligibleWaitlistEntries(slotId)

**Cross References:** UC9: Offer Slot to Waitlisted Patient. Also the result of cancelAppointment (CO6).

**Preconditions:** The ScheduleSlot exists and is available. The actor is authorized to manage the waitlist.

**Postconditions:** No instances, attributes or associations were changed.

**Result:** The waiting WaitlistEntries whose requested professional is the slot's professional or is unspecified, whose requested specialty is one of that professional's specialties or is unspecified, and whose date range contains the slot's date or is unspecified. Entries are ordered by priority, then by dateAdded, and include the patient's name and phone number.

&nbsp;

### CO14: offerSlot

**Operation:** offerSlot(entryId, slotId)

**Cross References:** UC9: Offer Slot to Waitlisted Patient.

**Preconditions:** The WaitlistEntry is waiting and eligible for the ScheduleSlot. The ScheduleSlot is available.

**Postconditions On Success:**
- we.status was set to offered. (attribute modification)
- we was associated with the ScheduleSlot (is offered). (association formed)
- The ScheduleSlot's isAvailable was set to false so that it cannot be booked for another patient during the offer. (attribute modification)
- An AuditEntry instance was created to record the offer and was associated with the acting UserAccount and the Patient.

**Failure:** If the entry is not waiting or the slot is no longer available, nothing is changed and the reason is returned.

&nbsp;

### CO15: acceptOffer

**Operation:** acceptOffer(entryId, serviceName)

**Cross References:** UC9: Offer Slot to Waitlisted Patient.

**Preconditions:** The WaitlistEntry is offered and is associated with a ScheduleSlot. The MedicalService exists.

**Postconditions On Success:**
- An Appointment instance a was created with status booked and channel phone, and was associated with the entry's Patient, the offered ScheduleSlot and the MedicalService, with the resource allocation postconditions of confirmBooking (CO3.4). The offered ScheduleSlot's isAvailable remained false. (instance creation, attribute modification, associations formed)
- we.status was set to fulfilled. (attribute modification)
- The association between we and the ScheduleSlot was removed. (association broken)
- An AuditEntry instance was created to record the booking and was associated with the acting UserAccount and the Patient.

**Failure:** If a required Resource has no free capacity, no appointment is created, we.status is set back to waiting, the association with the ScheduleSlot is removed and the slot's isAvailable is set to true. The reason is returned.

&nbsp;

### CO16: declineOffer

**Operation:** declineOffer(entryId)

**Cross References:** UC9: Offer Slot to Waitlisted Patient.

**Preconditions:** The WaitlistEntry is offered and is associated with a ScheduleSlot.

**Postconditions:**
- we.status was set to waiting. Its priority was unchanged. (attribute modification)
- The association between we and the ScheduleSlot was removed. (association broken)
- The ScheduleSlot's isAvailable was set to true. (attribute modification)
- An AuditEntry instance was created to record the refusal and was associated with the acting UserAccount and the Patient.

&nbsp;

### CO17: getWaitlist

**Operation:** getWaitlist(criteria)

**Cross References:** UC10: Change Waitlist Priority, UC11: Remove Patient from Waitlist.

**Preconditions:** The actor is authorized to manage the waitlist.

**Postconditions:** No instances, attributes or associations were changed.

**Result:** The active WaitlistEntries ordered by priority, optionally filtered by professional or specialty, with identifier, patient, request and priority.

&nbsp;

### CO18: changeWaitlistPriority

**Operation:** changeWaitlistPriority(entryId, newPriority)

**Cross References:** UC10: Change Waitlist Priority.

**Preconditions:** The WaitlistEntry exists and the actor is authorized to manage the waitlist.

**Postconditions On Success:**
- we.priority was set to newPriority. (attribute modification)
- The priority of the other active entries was adjusted where needed so that the ordering of the Waitlist stays unambiguous. (attribute modification)
- An AuditEntry instance was created to record the old and new priority and was associated with the acting UserAccount and the Patient.

**Failure:** If the entry is fulfilled or removed, or newPriority is not a valid value, the Waitlist is unchanged and the reason is returned.

&nbsp;

### CO19: removeFromWaitlist

**Operation:** removeFromWaitlist(entryId, reason)

**Cross References:** UC11: Remove Patient from Waitlist, UC9: Offer Slot to Waitlisted Patient (extension 5a).

**Preconditions:** The WaitlistEntry exists and the actor is authorized to manage the waitlist.

**Postconditions On Success:**
- we.status was set to removed. The entry was retained and stayed associated with the Waitlist and the Patient. (attribute modification)
- If we was associated with an offered ScheduleSlot, that association was removed and the slot's isAvailable was set to true. (association broken, attribute modification)
- An AuditEntry instance was created to record the removal and its reason and was associated with the acting UserAccount and the Patient.

**Failure:** If the entry is already fulfilled or removed, nothing is changed and its current status is returned.
