package clinic.controller;

import clinic.domain.AvailabilitySlot;
import clinic.domain.Clinic;
import clinic.domain.MedicalService;
import clinic.domain.ResourcePool;
import clinic.domain.SearchCriteria;

import java.util.List;

/** GRASP Controller for the "Search Availability" use case (system operation searchAvailability). */
public class AvailabilitySearchController {
    private final Clinic clinic;

    public AvailabilitySearchController(Clinic clinic) {
        this.clinic = clinic;
    }

    /**
     * Returns open slots matching the criteria. When a medical service is named,
     * slots whose required resources are already taken are left out.
     */
    public List<AvailabilitySlot> searchAvailability(SearchCriteria criteria) {
        List<AvailabilitySlot> candidates = clinic.getSchedule().findOpenSlots(criteria);
        if (criteria.serviceName() == null) {
            return candidates;
        }
        MedicalService service = clinic.findService(criteria.serviceName());
        ResourcePool pool = clinic.getResources();
        return candidates.stream()
                .filter(slot -> pool.canSatisfy(service.getRequirements(), slot.getTime()))
                .toList();
    }
}
