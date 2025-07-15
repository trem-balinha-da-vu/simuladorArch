package components;
import components.Bus;

public class Register {
    private String registerName;
    private int data;
    private Bus busInt;
    private Bus busExt;

    /**
	 * Default constructor
	 * @param busInt
     * @param busExt
     * @param registerName
     * @param data
	*/
    public Register(String registerName, Bus busInt, Bus busExt) {
        this.registerName = registerName;
        this.busInt = busInt;
        this.busExt = busExt;
    }

    private int flagBits[];
	private int numFlags;

    /**
	 * This special constructor is used to make Flags register
	 * with special bits for special informations
	 * @param numberOfBits
	 * @param bus
	 */
	public Register(int numberOfBits, Bus bus) {
		super();
		this.registerName = "Flags";
		this.numFlags = numberOfBits;
		this.flagBits = new int[numFlags];
		for (int i=0;i<numFlags;i++) {
			flagBits[i] = 0;
		}
		this.busInt = bus;
	}

    public int getData(){ return this.data; }

    public String getRegisterName() { return registerName; }

    public int getBit(int pos) { return flagBits[pos]; }

    /**
	 * This method allows the ULA to set any special bit
	 * @param pos
     * @param bit
	 */
	public void setBit(int pos, int bit) {
		flagBits[pos] = bit;
	}

    /**
     *  This method stores the data from the bus into this register
     */
    public void store(){
        this.data = busExt.getData();
    }

    public void read(){
        this.busExt.setData(this.data);
    }

    public void internalRead() {
        this.busInt.setData(this.data);
    }

    public void internalStore() {
        this.data = this.busInt.getData();
    }

    public void setData(int data) {
        this.data = data;
    }
}
