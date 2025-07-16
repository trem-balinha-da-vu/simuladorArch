package components;

public class Memory {
    private Bus extBus;
    private int storePosition; // this value indicates that the memory has read an address and is waiting for a
                               // data to be storesd in this position
    private int size;
    private int dataList[];

    public Memory(int size, Bus bus) {
        storePosition = -1; // negative values indicates the memory is not storing
        this.size = size;
        dataList = new int[size];
        this.extBus = bus;
        for (int i = 0; i < size; i++) {
            dataList[i] = 0;
        }
    }

	public int[] getDataList() {
		return dataList;
	}

    /**
	 * This method stores into position the data found in the bus
	 */
    public void store() {
		if (storePosition < 0) { //the storing is just starting
			this.storePosition = extBus.get();
		}
		else {//the storing was initiated, in the bus is the data
			this.dataList[storePosition] = extBus.get();
			storePosition = -1; //no storing is being performed anymore
		}
	}
	
	/**
	 * This method gets the data from the position and stores it into the bus
	 */
	public void read() {
		if ((extBus.get() < size)&&(extBus.get() >=0))
			extBus.put(dataList[extBus.get()]);
	}

    	/**
	 * Special method used in statusm memory to store the data in the position 0
	 */
	public void storeIn0() { 
		this.dataList[0] = extBus.get();
	}

	/**
	 * Special method used in statusm memory to store the data in the position 1
	 */
	public void storeIn1() { 
		this.dataList[1] = extBus.get();
	}

	public void directWrite(int address, int data) {
		if (address >= 0 && address < this.size) {
			this.dataList[address] = data;
		}
	}
}
