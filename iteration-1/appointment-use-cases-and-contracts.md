# Appointment Tracking System
## Iteration I - Use Cases and Operation Contracts

### UC1: Book Appointment

**Primary Actor:** Administrative Staff. A patient may also book directly when authorized.

**Stakeholders and Interests:**
- **Patient:** wants an appointment for the required service at a suitable time.
- **Administrative Staff:** wants to book for the correct patient without errors.
- **Healthcare Professional:** wants appointments to respect their availability.
- **Clinic:** wants to prevent conflicting bookings and enforce referral requirements.

**Preconditions:** The actor is authenticated and authorized to book appointments. Services, professionals, locations and schedules are recorded in the system.

**Success Guarantee:** A confirmed appointment is recorded for the patient, professional, location and service. A referral is linked when required. The selected interval is reserved and the booking is logged.

**Main Success Scenario:**
1. Staff enter the patient's search criteria.
2. The system displays matching patients. Staff identify the correct patient.
3. Staff enter the requested service, professional, location and preferred date.
4. The system displays suitable available intervals.
5. Staff select an interval and submit the booking details, including a referral when required.
6. The system checks current availability, scheduling constraints and the referral requirement.
7. The system records the appointment, reserves the interval and logs the booking.
8. The system displays the appointment identifier and confirmation details.

**Extensions:**
- **2a. Patient Not Found:** Staff correct the search criteria and retry, or register the patient through Register Patient before continuing.
- **2b. Multiple Matches:** Staff verify identifying information before selecting the patient.
- **4a. No Available Interval:** Staff request another date or end the request without booking.
- **6a. Interval No Longer Available (Failure):** The system rejects the booking. No appointment is created or interval reserved. Staff select another interval or end the request.
- **6b. Required Referral Missing or Unacceptable (Failure):** The system explains the referral requirement. No appointment is created. Staff provide an acceptable referral or end the request.
- **6c. Scheduling Constraint Violated:** The system rejects the request and explains the reason. Staff revise the request or end it.

**Special Requirements:** Access to patient information is restricted and logged. Concurrent requests must not produce conflicting bookings.

**Technology and Data Variations:** Patient searches support insurance number, date of birth, phone number, or partial first and/or last name. Patients without an insurance number can still be registered and identified. Interaction is through the CLI.

**Open Issues:** Exact scheduling constraints and referral validity rules, whether booking requires a separate confirmation step.

&nbsp;

### UC2: Reschedule Appointment

**Primary Sctor:** Administrative Staff. A patient may also reschedule their own appointment when authorized.

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
7. The system updates the appointment, releases the old interval, reserves the new one and logs the change.
8. The system displays the updated appointment confirmation.

**Extensions:**
- **2a. Appointment Not Found:** The system reports the error. Staff correct the identifier or end the request.
- **4a. No Available Interval:** Staff request another date or end the request. The original booking is retained.
- **6a. New Interval No Longer Available (Failure):** The system rejects the change. The original appointment and reserved interval remain unchanged.
- **6b. Appointment Not Eligible:** The system rejects the change if the appointment is no longer future and confirmed, or arrival has been registered.
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
5. The system marks the appointment cancelled, releases its interval and logs the cancellation.
6. The system displays cancellation confirmation.

**Extensions:**
- **2a. Appointment Not Found (Failure):** The system reports the error. Staff correct the identifier or end the request. Nothing is cancelled.
- **3a. Patient Decides to Keep the Appointment:** Staff do not submit cancellation. The booking remains unchanged.
- **4a. Appointment Already Cancelled:** The system reports its current status without changing availability again.
- **4b. Appointment Not Eligible (Failure):** The system rejects cancellation if the appointment is no longer future and confirmed, or arrival has been registered. Its state remains unchanged.

**Special Requirements:** Cancellation retains appointment history and associated clinical and billing information. Only authorized users may cancel appointments.

**Technology and Data Variations:** The appointment is retrieved by its unique identifier. Confirmation is displayed through the CLI.

**Open Issues:** Cancellation deadlines, fees and whether a cancellation reason is required.

&nbsp;

## System Operations

| Operation | Use case |
| --- | --- |
| searchPatient(criteria) | UC1 |
| findAvailableSlots(serviceCode, professionalId, locationId, date) | UC1, UC2 |
| bookAppointment(patientId, serviceCode, professionalId, locationId, startTime, referralId) | UC1 |
| getAppointment(appointmentId) | UC2, UC3 |
| rescheduleAppointment(appointmentId, newStartTime) | UC2 |
| cancelAppointment(appointmentId) | UC3 |

`referralId` is optional for services not requiring a referral. The acting UserAccount is identified by the authenticated session.

&nbsp;


## Operation Contracts

For these scenarios, successful bookings have status `confirmed`. Rescheduling and cancellation apply to future, confirmed appointments before arrival. These are initial assumptions pending clarification of the open issues above. A ScheduleSlot is identified by its professional, location and start date/time. Its duration is determined from its start and end times.

&nbsp;

### CO1: searchPatient

**Operation:** searchPatient(criteria)

**Cross References:** UC1: Book Appointment.

**Preconditions:** The actor is authenticated and authorized to search patient information.

**Postconditions:**
- Patient information was unchanged.
- For each patient whose identifying information was displayed, an AuditEntry instance was created, its timestamp, action and details were set, and it was associated with the acting UserAccount and that Patient.

**Result:** Matching patient summaries, including patient identifiers, or an empty result. Criteria may contain insurance number, date of birth, phone number or partial names.

&nbsp;


### CO2: findAvailableSlots

**Operation:** findAvailableSlots(serviceCode, professionalId, locationId, date)

**Cross References:** UC1: Book Appointment, UC2: Reschedule Appointment.

**Preconditions:** The actor is authorized to view availability. The specified MedicalService, HealthcareProfessional and ClinicLocation exist.

**Postconditions:** No instances, attributes or associations were changed. No interval was reserved.

**Result:** Suitable available intervals, or an empty result. Availability considers schedules, service duration, location and existing active appointments. Returned availability does not guarantee that a later booking will succeed.

&nbsp;


### CO3: bookAppointment

**Operation:** bookAppointment(patientId, serviceCode, professionalId, locationId, startTime, referralId)

**Cross References:** UC1: Book Appointment.

**Preconditions:** The actor is authorized to book for the patient. The specified Patient, MedicalService, HealthcareProfessional and ClinicLocation exist.

**Postconditions On Success:**
- An Appointment instance a was created. (instance creation)
- a.appointmentId was assigned a unique value, a.dateTime was set to startTime, a.duration was set to the interval duration, a.reason was set to the service name, a.status was set to confirmed, a.arrivalTime was unset. (attribute modification)
- a was associated with the specified Patient, HealthcareProfessional, ClinicLocation and MedicalService. (associations formed)
- a was associated with the supplied, accepted Referral when applicable. (association formed)
- The selected ScheduleSlot's isAvailable was set to false. (attribute modification)
- An AuditEntry instance ae was created. Its timestamp, action and details were set to record the booking time, activity and appointment identifier. ae was associated with the acting UserAccount and Patient. (instance creation, attribute modification, associations formed)

**Failure:** If the interval is unavailable, a scheduling constraint is violated or a required referral is unacceptable, no appointment is created and no scheduling state is changed. The rejection reason is returned.

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
- a.status remained confirmed. Its patient, professional, location, service and referral associations were unchanged.
- The old ScheduleSlot's isAvailable was set to true and the new ScheduleSlot's isAvailable was set to false. (attribute modification)
- An AuditEntry instance ae was created. Its timestamp, action and details were set to record the change, appointment identifier and old and new times. ae was associated with the acting UserAccount and Patient. (instance creation, attribute modification, associations formed)

**Failure:** If the appointment is ineligible, the new interval is unavailable or a scheduling/referral requirement fails, the appointment and slot availability remain unchanged. The rejection reason is returned.

&nbsp;

### CO6: cancelAppointment

**Operation:** cancelAppointment(appointmentId)

**Cross References:** UC3: Cancel Appointment.

**Preconditions:** The Appointment exists and the actor is authorized to manage it.

**Postconditions On Success:**
- The Appointment was retained and its status was set to cancelled. (attribute modification)
- The corresponding ScheduleSlot's isAvailable was set to true. (attribute modification)
- The appointment's patient, professional, location, service and referral associations were unchanged. No related clinical or billing information was deleted.
- An AuditEntry instance ae was created. Its timestamp, action and details were set to record the cancellation and appointment identifier. ae was associated with the acting UserAccount and Patient. (instance creation, attribute modification, associations formed)

**Failure:** If the appointment is already cancelled or otherwise ineligible, its state and slot availability remain unchanged. The current status or rejection reason is returned.
