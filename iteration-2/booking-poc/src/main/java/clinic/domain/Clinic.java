package clinic.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Root domain object. It owns the catalogs; controllers get them from here.
 * The PoC starts empty (nothing in memory) and is filled by CSV import or SampleData.
 */
public class Clinic {
    private final List<Professional> professionals = new ArrayList<>();
    private final List<MedicalService> services = new ArrayList<>();
    private final Schedule schedule = new Schedule();
    private final PatientRegistry patients = new PatientRegistry();
    private final AppointmentBook appointments = new AppointmentBook();
    private final ResourcePool resources = new ResourcePool();

    public Professional addProfessional(String firstName, String lastName, String title, String specialty) {
        Professional p = new Professional("P" + (professionals.size() + 1), firstName, lastName, title, specialty);
        professionals.add(p);
        return p;
    }

    public Optional<Professional> findProfessional(String name) {
        return professionals.stream().filter(p -> p.hasName(name)).findFirst();
    }

    public MedicalService addService(String name, List<ResourceRequirement> requirements) {
        MedicalService s = new MedicalService(name, requirements);
        services.add(s);
        return s;
    }

    public MedicalService findService(String name) {
        return services.stream().filter(s -> s.getName().equalsIgnoreCase(name)).findFirst()
                .orElseThrow(() -> new BookingException("Unknown medical service: " + name));
    }

    public List<Professional> getProfessionals() { return List.copyOf(professionals); }
    public List<MedicalService> getServices() { return List.copyOf(services); }
    public Schedule getSchedule() { return schedule; }
    public PatientRegistry getPatients() { return patients; }
    public AppointmentBook getAppointments() { return appointments; }
    public ResourcePool getResources() { return resources; }
}
