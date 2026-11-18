package clinic.domain;

/**
 * A predefined resource that some medical services need besides the professional.
 * ROOM and EQUIPMENT normally have capacity 1. A ROLE (nurse, assistant...) is not
 * tied to a specific person: its capacity is how many people in that role are on shift.
 */
public class Resource {

    public enum Kind { ROOM, EQUIPMENT, ROLE }

    private final String name;
    private final Kind kind;
    private final int capacity;

    public Resource(String name, Kind kind, int capacity) {
        if (capacity < 1) throw new IllegalArgumentException("capacity must be >= 1");
        this.name = name;
        this.kind = kind;
        this.capacity = capacity;
    }

    public String getName() { return name; }
    public Kind getKind() { return kind; }
    public int getCapacity() { return capacity; }

    @Override
    public String toString() {
        return name + " (" + kind + (capacity > 1 ? " x" + capacity : "") + ")";
    }
}
