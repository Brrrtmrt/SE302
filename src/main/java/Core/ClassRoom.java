package Core;

public class ClassRoom {
    private String name;
    private int capacity;

    public ClassRoom(String name, int capacity) {
        this.name = name;
        this.capacity = capacity;
    }

    public String getName() {
        return name;
    }

    public int getCapacity() {
        return capacity;
    }
}