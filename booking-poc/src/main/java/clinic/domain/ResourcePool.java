package clinic.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Pure Fabrication: owns the predefined resources and every active allocation,
 * and answers "is this resource free at that time?".
 */
public class ResourcePool {
    private final List<Resource> resources = new ArrayList<>();
    private final List<ResourceAllocation> allocations = new ArrayList<>();

    public Resource addResource(String name, Resource.Kind kind, int capacity) {
        Resource r = new Resource(name, kind, capacity);
        resources.add(r);
        return r;
    }

    public Optional<Resource> findResource(String name) {
        return resources.stream().filter(r -> r.getName().equalsIgnoreCase(name)).findFirst();
    }

    /** Units of the resource still free during the given time. */
    public int freeUnits(Resource resource, TimeSlot time) {
        int used = allocations.stream()
                .filter(a -> a.resource() == resource && a.time().overlaps(time))
                .mapToInt(ResourceAllocation::quantity).sum();
        return resource.getCapacity() - used;
    }

    public boolean canSatisfy(List<ResourceRequirement> requirements, TimeSlot time) {
        return requirements.stream().allMatch(req -> freeUnits(req.resource(), time) >= req.quantity());
    }

    /** Reserves every requirement for the time, or nothing at all. */
    public List<ResourceAllocation> allocate(List<ResourceRequirement> requirements, TimeSlot time) {
        if (!canSatisfy(requirements, time)) {
            throw new BookingException("Required resources are not available on " + time + ": " + requirements);
        }
        List<ResourceAllocation> created = new ArrayList<>();
        for (ResourceRequirement req : requirements) {
            ResourceAllocation a = new ResourceAllocation(req.resource(), req.quantity(), time);
            allocations.add(a);
            created.add(a);
        }
        return created;
    }

    /** Undo of allocate (used if confirming the appointment fails). */
    public void release(Collection<ResourceAllocation> toRelease) {
        // remove by identity: two equal records may belong to different appointments
        for (ResourceAllocation a : toRelease) {
            for (int i = 0; i < allocations.size(); i++) {
                if (allocations.get(i) == a) {
                    allocations.remove(i);
                    break;
                }
            }
        }
    }

    public List<Resource> getResources() {
        return List.copyOf(resources);
    }

    public List<ResourceAllocation> getAllocations() {
        return List.copyOf(allocations);
    }
}
