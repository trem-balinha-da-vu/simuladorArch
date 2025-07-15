package architecture;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
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

    // Função auxiliar para reusar a lógica de incremento do PC
    private void incrementPC() {
        PC.internalRead();      // intBus1 <- [PC]
        ula.internalStore(1);   // ula.reg2 <- [PC] (usa ula.reg2 como temporário)
        ula.inc();              // ula.reg2 <- [PC] + 1
        ula.internalRead(1);    // intBus1 <- [PC] + 1
        PC.internalStore();     // PC <- [PC] + 1
    }

    /**
     * add %regA %regB => regB <- regA + regB
     */
    public void addRegReg() { //1 323 344
        incrementPC();
        
        // 2. BUSCA O ID DO regA DA MEMÓRIA
        PC.internalRead();      // intBus1 <- [PC] (endereço do ID de regA)
        ula.internalStore(0);   // ula.reg1 <- [PC] (move para a ULA para acessar o extBus)
        ula.read(0);            // extBus <- [PC] (coloca o endereço no barramento externo)
        memory.read();          // Memória lê o endereço do extBus e coloca o DADO (ID do regA) de volta no extBus
        
        // Agora, o ID do regA está no extBus. Vamos usá-lo para pegar o CONTEÚDO de regA.
        demux.setValue(extBus.get()); // demux <- ID do regA
        registersInternalRead();      // intBus1 <- [conteúdo do regA] (Lê do registrador selecionado pelo demux)
        ula.internalStore(0);         // ula.reg1 <- [conteúdo do regA] (Guarda o primeiro operando na ULA)

        // 3. INCREMENTA PC PARA APONTAR PARA O ID DO regB
        incrementPC();

        // 4. BUSCA O ID DO regB DA MEMÓRIA E O CONTEÚDO DO regB
        PC.internalRead();      // intBus1 <- [PC] (endereço do ID de regB)
        ula.internalStore(1);   // ula.reg2 <- [PC] (usa o outro registrador da ULA)
        ula.read(1);            // extBus <- [PC]
        memory.read();          // extBus <- ID do regB (dado lido da memória)
        
        demux.setValue(extBus.get()); // demux <- ID do regB (IMPORTANTE: demux agora aponta para regB, que é nosso destino final)
        registersInternalRead();      // intBus1 <- [conteúdo do regB]
        ula.internalStore(1);         // ula.reg2 <- [conteúdo do regB] (Guarda o segundo operando na ULA)

        // 5. EXECUTA A SOMA E GRAVA O RESULTADO EM regB
        ula.add();                    // Executa a soma: ula.reg2 <- ula.reg1 + ula.reg2
        setStatusFlags(intBus1.get());// Atualiza as flags com o resultado que a ULA colocou no intBus1
        
        ula.internalRead(1);          // intBus1 <- [resultado da soma] (lê o resultado de ula.reg2)
        // O demux ainda está com o valor do ID do regB, então a escrita será no lugar certo.
        registersInternalStore();     // regB <- [resultado da soma]

        // 6. INCREMENTA PC PARA APONTAR PARA A PRÓXIMA INSTRUÇÃO
        incrementPC();
    }

    /**
     * Microprograma para: add <mem> %regA => regA <- MEM[mem] + regA
     */
    public void addMemReg() {
        // 1. BUSCA O OPERANDO DA MEMÓRIA (Acesso Indireto)
        incrementPC(); // PC aponta para o endereço <mem> (em N+1)
        
        PC.internalRead();      // intBus1 <- [PC]
        ula.internalStore(0);   // ula.reg1 <- [PC]
        ula.read(0);            // extBus <- [PC] (endereço do operando <mem>)
        memory.read();          // extBus <- MEM[[PC]] (o valor de <mem>, o endereço efetivo)
        // Neste ponto, extBus contém o endereço efetivo. Usamos ele para buscar o dado final.
        memory.read();          // extBus <- MEM[<mem>] (o valor final do primeiro operando)
        ula.store(0);           // ula.reg1 <- [valor de MEM[<mem>]]

        // 2. BUSCA O OPERANDO DO REGISTRADOR
        incrementPC(); // PC aponta para o ID de %regA (em N+2)

        PC.internalRead();      // intBus1 <- [PC]
        ula.internalStore(1);   // ula.reg2 <- [PC]
        ula.read(1);            // extBus <- [PC] (endereço do ID de regA)
        memory.read();          // extBus <- ID de regA
        
        demux.setValue(extBus.get()); // demux <- ID de regA
        registersInternalRead();      // intBus1 <- [conteúdo de regA]
        ula.internalStore(1);         // ula.reg2 <- [conteúdo de regA]

        // 3. EXECUTA A SOMA E GRAVA O RESULTADO
        ula.add();                      // ula.reg2 <- ula.reg1 + ula.reg2
        setStatusFlags(intBus1.get());  // Atualiza flags
        
        ula.internalRead(1);            // intBus1 <- [resultado]
        // O demux ainda aponta para regA, então a escrita será no destino correto.
        registersInternalStore();       // regA <- [resultado]

        // 4. INCREMENTA PC PARA A PRÓXIMA INSTRUÇÃO
        incrementPC();
    }

    /**
     * Microprograma para: add %regA <mem> => MEM[mem] <- regA + MEM[mem]
     */
    public void addRegMem() {
        // 1. BUSCA O OPERANDO DO REGISTRADOR (%regA)
        incrementPC(); // PC aponta para o ID de %regA (em N+1)
        
        PC.internalRead();      // intBus1 <- [PC]
        ula.internalStore(0);   // ula.reg1 <- [PC]
        ula.read(0);            // extBus <- [PC] (endereço do ID de regA)
        memory.read();          // extBus <- ID de regA
        
        demux.setValue(extBus.get()); // demux <- ID de regA
        registersInternalRead();      // intBus1 <- [conteúdo de regA]
        ula.internalStore(0);         // ula.reg1 <- [conteúdo de regA] (Primeiro operando na ULA)

        // 2. BUSCA O OPERANDO DA MEMÓRIA (MEM[mem])
        incrementPC(); // PC aponta para o endereço <mem> (em N+2)

        PC.internalRead();      // intBus1 <- [PC]
        ula.internalStore(1);   // ula.reg2 <- [PC]
        ula.read(1);            // extBus <- [PC] (endereço do operando <mem>)
        memory.read();          // extBus <- MEM[[PC]] (o valor de <mem>, o endereço efetivo)
        memory.read();          // extBus <- MEM[<mem>] (o valor final do segundo operando)
        ula.store(1);           // ula.reg2 <- [valor de MEM[<mem>]]

        // 3. EXECUTA A SOMA
        ula.add();
        setStatusFlags(intBus1.get());

        // 4. ARMAZENA O RESULTADO DE VOLTA NA MEMÓRIA
        // O resultado está em ula.reg2. Precisamos do endereço <mem> novamente.
        // Como o PC ainda aponta para N+2, podemos buscar o endereço <mem> de novo.
        PC.internalRead();      // intBus1 <- [PC] (N+2)
        ula.internalStore(0);   // ula.reg1 <- [PC] (Usa ula.reg1 como temporário)
        ula.read(0);            // extBus <- [PC] (endereço de <mem>)
        memory.read();          // extBus <- <mem> (o endereço efetivo, nosso destino)
        
        // Agora extBus tem o endereço de destino, inicia o processo de escrita
        memory.store();         // 1ª parte da escrita: a memória salva o endereço de destino <mem>
        
        // Coloque o resultado da soma no barramento externo
        ula.read(1);            // extBus <- [resultado] (lê de ula.reg2, onde a soma foi salva)
        memory.store();         // 2ª parte da escrita: a memória grava o dado no endereço salvo

        // 5. INCREMENTA O PC PARA A PRÓXIMA INSTRUÇÃO
        incrementPC();
    }

    /**
     * Microprograma para: add imm %regA => regA <- imm + regA
     */
    public void addImmReg() {
        // 1. BUSCA O VALOR IMEDIATO DA MEMÓRIA
        incrementPC(); // PC aponta para o valor imediato (em N+1)
        
        PC.internalRead();      // intBus1 <- [PC]
        ula.internalStore(0);   // ula.reg1 <- [PC]
        ula.read(0);            // extBus <- [PC] (endereço do valor imediato)
        memory.read();          // extBus <- [valor imediato]
        ula.store(0);           // ula.reg1 <- [valor imediato] (Primeiro operando na ULA)

        // 2. BUSCA O OPERANDO DO REGISTRADOR
        incrementPC(); // PC aponta para o ID de %regA (em N+2)

        PC.internalRead();      // intBus1 <- [PC]
        ula.internalStore(1);   // ula.reg2 <- [PC]
        ula.read(1);            // extBus <- [PC] (endereço do ID de regA)
        memory.read();          // extBus <- ID de regA
        
        demux.setValue(extBus.get()); // demux <- ID de regA
        registersInternalRead();      // intBus1 <- [conteúdo de regA]
        ula.internalStore(1);         // ula.reg2 <- [conteúdo de regA] (Segundo operando na ULA)

        // 3. EXECUTA A SOMA E GRAVA O RESULTADO
        ula.add();                      // ula.reg2 <- ula.reg1 + ula.reg2
        setStatusFlags(intBus1.get());  // Atualiza flags
        
        ula.internalRead(1);            // intBus1 <- [resultado]
        // O demux ainda aponta para regA, então a escrita será no destino correto.
        registersInternalStore();       // regA <- [resultado]

        // 4. INCREMENTA PC PARA A PRÓXIMA INSTRUÇÃO
        incrementPC();
    }

    // sub %regA %regB => regB <- regA - regB
    public void subRegReg() {
        // busca regA
        incrementPC();

        // 2. BUSCA O ID DO regA DA MEMÓRIA
        PC.internalRead();      // intBus1 <- [PC] (endereço do ID de regA)
        ula.internalStore(0);   // ula.reg1 <- [PC] (move para a ULA para acessar o extBus)
        ula.read(0);            // extBus <- [PC] (coloca o endereço no barramento externo)
        memory.read();          // Memória lê o endereço do extBus e coloca o DADO (ID do regA) de volta no extBus
        
        // Agora, o ID do regA está no extBus. Vamos usá-lo para pegar o CONTEÚDO de regA.
        demux.setValue(extBus.get()); // demux <- ID do regA
        registersInternalRead();      // intBus1 <- [conteúdo do regA] (Lê do registrador selecionado pelo demux)
        ula.internalStore(0);         // ula.reg1 <- [conteúdo do regA] (Guarda o primeiro operando na ULA)

        // 3. INCREMENTA PC PARA APONTAR PARA O ID DO regB
        incrementPC();

        // 4. BUSCA O ID DO regB DA MEMÓRIA E O CONTEÚDO DO regB
        PC.internalRead();      // intBus1 <- [PC] (endereço do ID de regB)
        ula.internalStore(1);   // ula.reg2 <- [PC] (usa o outro registrador da ULA)
        ula.read(1);            // extBus <- [PC]
        memory.read();          // extBus <- ID do regB (dado lido da memória)
        
        demux.setValue(extBus.get()); // demux <- ID do regB (demux agora aponta para regB, que é nosso destino final)
        registersInternalRead();      // intBus1 <- [conteúdo do regB]
        ula.internalStore(1);         // ula.reg2 <- [conteúdo do regB] (Guarda o segundo operando na ULA)

        // subtrai e grava em regB
        ula.sub();
        setStatusFlags(intBus1.get());

        ula.internalRead(1);
        registersInternalStore();

        // 6. INCREMENTA PC PARA APONTAR PARA A PRÓXIMA INSTRUÇÃO
        incrementPC();
    }


     /**
	 * This method performs an (external) read from a register into the register list.
	 * The register id must be in the demux bus
	 */
	private void registersRead() {
		registersList.get(demux.getValue()).read();
	}
	
	/**
	 * This method performs an (internal) read from a register into the register list.
	 * The register id must be in the demux bus
	 */
	private void registersInternalRead() {
		registersList.get(demux.getValue()).internalRead();
	}
	
	/**
	 * This method performs an (external) store to a register into the register list.
	 * The register id must be in the demux bus
	 */
	private void registersStore() {
		registersList.get(demux.getValue()).store();
	}
	
	/**
	 * This method performs an (internal) store to a register into the register list.
	 * The register id must be in the demux bus
	 */
	private void registersInternalStore() {
		registersList.get(demux.getValue()).internalStore();
	}

    /**
	 * Lê um arquivo executável (em linguagem de máquina) e guarda na memória.
     * Não testado ainda
	 */
	public void readExec(String filename) throws IOException {
		BufferedReader br = new BufferedReader(new		 
		FileReader(filename+".dxf"));
		String linha;
		int i=0;
		while ((linha = br.readLine()) != null) {
			extBus.put(i);
			memory.store();
			extBus.put(Integer.parseInt(linha));
			memory.store();
			i++;
		}
		br.close();
	}
    
    /**
     * Executa um programa guardado em memória
	 */
	public void controlUnitEexec() {
		halt = false;
		while (!halt) {
			//fetch();
			//decodeExecute();
		}
	}
	
    private void decodeExecute() {
		this.IR.internalRead(); //a instrução está no intBus1
		int command = this.intBus1.get();
		//simulationDecodeExecuteBefore(command);
		switch (command) {
		case 0:
			addRegReg();
			break;
		case 1:
			addMemReg();
			break;
		case 2:
			addRegMem();
			break;
		case 3:
			addImmReg();
			break;
		/*case 5:
			sub();
			break;
		*/
		case 4:
			subRegReg();
			break;
		
		case 5:
			subMemReg();
			break;
		
		case 6:
			subRegMem();
			break;
		
		case 7:
			subImmReg();
			break;
		
		case 8:
			imulMemReg(); //Linha 1310
			break;

		case 9:
			imulRegMem(); //Linha 1314
			break;

		case 10:
			imulRegReg(); //Linha 
			break;

		case 11:
			moveMemReg();
			break;

		case 12:
			moveRegMem();
			break;

		case 13:
			moveRegReg();
			break;
		
		case 14:
			moveImmReg();
			break;

		case 15:
			incReg(); 
			break;

		case 16:
			jmp();
			break;
	
		case 17:
			jn();
			break;

		case 18:
			jz();
			break;
			
		case 19:
			jeq();
			break;

		case 20:
			jneq();
			break;

		case 21:
			jgt();
			break;

		case 22:
			jlw();
			break;

		case 23:
			read();
			break;

		case 24:
			store();
			break;
			
		case 25:
			ldi();
			break;

		default:
			halt = true;
			break;
		}
		// if (simulation) {simulationDecodeExecuteAfter(); }
	}


    //Funções de simulação
    private void simulationDecodeExecuteBefore(int command) {
        if(this.simulation){
            System.out.println("----------BEFORE Decode and Execute phases--------------");
            int parameter = 0;
            Iterator<Register> var5 = this.registersList.iterator();

            while(var5.hasNext()) {
                Register r = (Register)var5.next();
                System.out.println(r.getRegisterName() + ": " + r.getData());
            }

            String instruction;
            if (command != -1) {
                instruction = (String)this.commandsList.get(command);
            } else {
                instruction = "END";
            }

            if (hasOperands(instruction)) {
                parameter = this.memory.getDataList()[this.PC.getData() + 1];
                System.out.println("Instruction: " + instruction + " " + parameter);
            } else {
                System.out.println("Instruction: " + instruction);
            }

            if ("read".equals(instruction)) {
                System.out.println("memory[" + parameter + "]=" + this.memory.getDataList()[parameter]);
            }
        }
    }

    private void simulationDecodeExecuteAfter() {
        if(this.simulation){
            System.out.println("-----------AFTER Decode and Execute phases--------------");
            System.out.println("Internal Bus 1: " + this.intBus1.get());
            System.out.println("External Bus 1: " + this.extBus.get());
            Iterator<Register> var3 = this.registersList.iterator();

            while(var3.hasNext()) {
                Register r = (Register)var3.next();
                System.out.println(r.getRegisterName() + ": " + r.getData());
            }

            Scanner entrada = new Scanner(System.in);
            System.out.println("Press <Enter>");
            String mensagem = entrada.nextLine();

            entrada.close();
        }
    }

    private void simulationFetch() {
        if (this.simulation) {
            System.out.println("-------Fetch Phase------");
            System.out.println("PC: " + this.PC.getData());
            System.out.println("IR: " + this.IR.getData());
        }
    }

    private void fetch() {
        this.PC.internalRead();
        this.memory.read();
        this.IR.internalStore();
        simulationFetch();
    }

    // Inc é a unica operação que nao tem operandos
    private boolean hasOperands(String instruction) {
        return !"inc".equals(instruction);
    }

    public int getMemorySize() {
		return memorySize;
	}

}






