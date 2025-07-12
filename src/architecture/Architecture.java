package architecture;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;
import components.*;

public class Architecture {

    private boolean simulation;
    private boolean halt;

    // Mudanças nos componentes da arquitetura
    // Temos dois barramentos: intBus1 e extBus
    private Bus extBus;
    private Bus intBus1;

    // Memória
    private Memory memory;
    private Memory statusMemory;
    private int memorySize;

    // Registradores
    private Register PC;
    private Register IR;
    private Register REG0;
    private Register REG1;
    private Register REG2;
    private Register REG3;
    private Register StkTOP;
    private Register StkBOT;
    private Register Flags;

    //ULA
    private ULA ula;

    //Demux
    private Demux demux;

    //Listas de comandos e registradores
    private ArrayList<String> commandsList;
    private ArrayList<Register> registersList;

    // Inicializa todos os componentes da arquitetura
    private void componentsInstances() {
        // buses -> registers -> ula -> memory
        //inicializa barramentos
        this.extBus = new Bus();
        this.intBus1 = new Bus();

        // Inicializa registradores
        // Ordem dos parâmetros do construtor de Register: (Register, BusInt e BusExt)
        // Os registradores estão com o terceiro parâmetro nulo porque não se conectam ao barramento externo
        IR = new Register("IR", intBus1, null);
        REG0 = new Register("RPG0", intBus1, null);
        REG1 = new Register("RPG1", intBus1, null);
        REG2 = new Register("RPG2", intBus1, null);
        REG3 = new Register("RPG3", intBus1, null);
        PC = new Register("PC", intBus1, null);
        StkTOP = new Register("stkTop", intBus1, null);
		StkBOT = new Register("stkBot", intBus1, null);

        //TODO: Certificar se realmente é pra conectar no intBus1.
        Flags = new Register(2, intBus1);

        fillRegistersList();

        ula = new ULA(intBus1, extBus);

        statusMemory = new Memory(2, extBus);
        
        memorySize = 128;
        memory = new Memory(memorySize, extBus);

        demux = new Demux();

        fillCommandsList();
    }

    // Preenche a lista de registradores
    private void fillRegistersList() {
        registersList = new ArrayList<Register>();

        registersList.add(this.IR);
        registersList.add(this.REG0);
        registersList.add(this.REG1);
        registersList.add(this.REG2);
        registersList.add(this.REG3);
        registersList.add(this.PC);
        registersList.add(this.StkTOP);
        registersList.add(this.StkBOT);
        registersList.add(this.Flags);
    }

    // Preenche a lista de comandos com os comandos do nosso assembler
    protected void fillCommandsList() {
        commandsList = new ArrayList<String>();
        commandsList.add("addRegReg");   		//0
        commandsList.add("addMemReg");   		//1
        commandsList.add("addRegMem");   		//2
        commandsList.add("addImmReg");    		//3
        commandsList.add("subRegReg");    		//4
        commandsList.add("subMemReg");  		//5
        commandsList.add("subRegMem"); 		    //6
        commandsList.add("subImmRegg");   		//7
        commandsList.add("imulMemReg");   		//8
        commandsList.add("imulRegMem");         //9
        commandsList.add("imulRegReg");         //10
        commandsList.add("moveMemReg");         //11
		commandsList.add("moveRegMem");         //12
		commandsList.add("moveRegReg");         //13
		commandsList.add("moveImmReg");         //14
        commandsList.add("incReg");	   	        //15
		commandsList.add("jmp");                //16
		commandsList.add("jn");                 //17
		commandsList.add("jz");                 //18
        commandsList.add("jeq");                //19
		commandsList.add("jneq");               //20
		commandsList.add("jgt");                //21
		commandsList.add("jlw");                //22
		commandsList.add("read");               //23
		commandsList.add("store");              //24
		commandsList.add("ldi");                //25
    }

    // O construtor que instancia todos os componentes de
    // Acordo com o diagrama da arquitetura
    public Architecture() {
        componentsInstances();

        // o modo de execuçao nunca é o modo de simulaçao, por padrao
        simulation = false;
    }

    public Architecture(boolean sim) {
        componentsInstances();

        // nesse construtor, nos podemos ligar ou desligar o modo de simulacao
        simulation = sim;
    }

    // Getters
    public Register getIR(){ return this.IR; }
    public Register getReg0() { return this.REG0; }
    public Register getReg1() { return this.REG1; }
    public Register getReg2() { return this.REG2; }
    public Register getReg3() { return this.REG3; }
    public Register getStkTOP() { return this.StkTOP; }
	public Register getStkBOT() { return this.StkBOT; }
	public Register getFlags() { return this.Flags; }
    public Bus getInBus1(){ return this.intBus1; }
    public Bus getExtBus(){ return this.extBus; }
    public ULA getUla() { return this.ula; }
    public Memory getMemory(){ return this.memory; }

    // metodo geral para registradores
    // eles so retornam listas, e nao alteram o estado diretamente
    public ArrayList<Register> getRegistersList() { return registersList; }

    public ArrayList<String> getCommandsList() { return commandsList; }
    
    // esse metodo é usado para algumas operacoes da ula,
    // configurando os bits da flags de acordo com o resultado.
    // NAO FOI TESTADO.
    private void setStatusFlags(int result) {
        Flags.setBit(0, 0);
        Flags.setBit(1, 0);
        if (result==0) { //bit 0 in flags must be 1 in this case
            Flags.setBit(0,1);
        }
        if (result<0) { //bit 1 in flags must be 1 in this case
            Flags.setBit(1,1);
        }
    }

    // os microprogramas ja estao implementados no arquivo de exemplo? sim
    // parei na linha 192 do exemplo de degas



    


}






