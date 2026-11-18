package clinic.domain;

/** "This service needs {quantity} of {resource}." */
public record ResourceRequirement(Resource resource, int quantity) {
    @Override
    public String toString() {
        return quantity + " x " + resource.getName();
    }
}
