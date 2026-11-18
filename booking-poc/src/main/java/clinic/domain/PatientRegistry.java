package clinic.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** All known patients. */
public class PatientRegistry {
    private final List<Patient> patients = new ArrayList<>();
    private int nextId = 1;

    /** Finds a patient by RAMQ number or, for patients without one, by telephone. */
    public Optional<Patient> find(String ramqOrPhone) {
        return patients.stream().filter(p -> p.isIdentifiedBy(ramqOrPhone)).findFirst();
    }

    /** Creator: the registry records patients, so it creates them. */
    public Patient register(String firstName, String lastName, String telephone, String ramqNo, String insurance) {
        Patient p = new Patient(nextId++, firstName, lastName, telephone, ramqNo, insurance);
        if (p.hasRamq() && find(p.getRamqNo()).isPresent()) {
            throw new BookingException("A patient with RAMQ " + p.getRamqNo() + " already exists");
        }
        patients.add(p);
        return p;
    }

    public List<Patient> getAll() {
        return List.copyOf(patients);
    }
}
