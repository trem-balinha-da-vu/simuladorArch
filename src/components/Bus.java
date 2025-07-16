package components;

public class Bus {

    public Bus() {
        this.data = 0;
    }
    private int data;

    public int get() {
        return this.data;
    }

    public void put(int data) {
        this.data = data;
    }
}
