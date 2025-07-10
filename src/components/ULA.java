package components;

public class ULA {
	private Bus intBus1;
	private Bus extBus;
	private Register reg1;
	private Register reg2;

    public ULA(Bus extBus) {
		super();
		this.extBus = extBus;
		intBus1 = new Bus();
		reg1 = new Register("UlaReg0", intBus1, extBus);
		reg2 = new Register("UlaReg1", intBus1, extBus);
	}

    //reg1 <- reg0 + reg1 
    public void add() {
        int res = 0; //false
        this.intBus1.setData(0); //limpar o bus ?? 
        this.reg1.internalRead();
        res = this.intBus1.getData();
        this.reg2.internalRead();
        res += this.intBus1.getData();
        this.intBus1.setData(res);
        this.reg2.internalStore();
    }

    //reg1 <- reg0 + reg1
    public void sub() {
        int res = 0; //false
        this.intBus1.setData(0);
        this.reg1.internalRead();
        res = this.intBus1.getData();
        this.reg2.internalRead();
        res -= this.intBus1.getData();
        this.intBus1.setData(res);
        this.reg2.internalStore();
    }

    public void inc() {
        this.reg2.internalRead();
        int res = this.intBus1.getData();
        ++res;
        this.intBus1.setData(res);
        this.reg2.internalStore();
    }

    //Engole do barramento externo
    public void store(int reg) {
        if (reg == 0) {
            this.reg1.store();
        } else {
            this.reg2.store();
        }
    }

    //Cospe no barramento externo
    public void read(int reg) {
        if (reg == 0) {
            this.reg1.read();
        } else {
            this.reg2.read();
        }
    }

    public void internalStore(int reg) {
        if (reg == 0) {
            this.reg1.internalStore();
        } else {
            this.reg2.internalStore();
        }
    }

    public void internalRead(int reg) {
        if (reg == 0) {
            this.reg1.internalRead();
        } else {
            this.reg2.internalRead();
        }
    }
}
