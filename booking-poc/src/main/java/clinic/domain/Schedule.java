package clinic.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * All availability slots of the clinic (the "availability database").
 * Make Availability and Import from CSV add slots here; Export to CSV reads getAllSlots().
 */
public class Schedule {
    private final List<AvailabilitySlot> slots = new ArrayList<>();
    private int nextId = 1;

    /** Creator: Schedule records slots, so it creates them. */
    public AvailabilitySlot addSlot(Professional professional, TimeSlot time) {
        for (AvailabilitySlot s : slots) {
            if (s.getProfessional() == professional && s.getTime().overlaps(time)) {
                throw new BookingException(professional.getFullName() + " already has a slot overlapping " + time);
            }
        }
        AvailabilitySlot slot = new AvailabilitySlot(nextId++, professional, time);
        slots.add(slot);
        return slot;
    }

    public AvailabilitySlot findSlot(int slotId) {
        return slots.stream().filter(s -> s.getId() == slotId).findFirst()
                .orElseThrow(() -> new BookingException("No availability slot #" + slotId));
    }

    /** Open slots that match the criteria, in chronological order. */
    public List<AvailabilitySlot> findOpenSlots(SearchCriteria criteria) {
        return slots.stream()
                .filter(AvailabilitySlot::isOpen)
                .filter(s -> s.matches(criteria))
                .sorted(Comparator.comparing((AvailabilitySlot s) -> s.getTime().date())
                        .thenComparing(s -> s.getTime().start()))
                .toList();
    }

    public List<AvailabilitySlot> getAllSlots() {
        return List.copyOf(slots);
    }
}
