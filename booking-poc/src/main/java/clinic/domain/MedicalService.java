package clinic.domain;

import java.util.List;

/** A kind of appointment (consultation, ECG, ...) and the resources it needs. */
public class MedicalService {
    private final String name;
    private final List<ResourceRequirement> requirements;

    public MedicalService(String name, List<ResourceRequirement> requirements) {
        this.name = name;
        this.requirements = List.copyOf(requirements);
    }

    public String getName() { return name; }
    public List<ResourceRequirement> getRequirements() { return requirements; }

    public boolean needsResources() {
        return !requirements.isEmpty();
    }

    @Override
    public String toString() {
        return name + (needsResources() ? " " + requirements : "");
    }
}
