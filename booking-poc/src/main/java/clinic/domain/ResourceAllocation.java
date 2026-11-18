package clinic.domain;

/** A quantity of a resource reserved for a time slot (held by an Appointment). */
public record ResourceAllocation(Resource resource, int quantity, TimeSlot time) {
    @Override
    public String toString() {
        return quantity + " x " + resource.getName() + " @ " + time;
    }
}
