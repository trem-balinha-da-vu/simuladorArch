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
        setupIMul(); 
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

    /**
     * Define a sub-rotina de multiplicação.
     * O final da rotina (epílogo) é deixado em branco para ser preenchido
     * dinamicamente pelos microprogramas 'imul'.
     */
    public void setupIMul() {
        // --- Definições ---
        int returnAddr = 121, resultAddr = 123, yAddr = 124, xAddr = 125, tempReg0 = 126, tempReg1 = 127;
        int reg0_id = 1, reg1_id = 2, pc_id = 5;
        int loopAddr = 88, endLoopAddr = 111, epilogueAddr = 114;

        // --- Código de Máquina da Sub-rotina Principal ---
        int[] subroutineMachineCode = {
            // --- Setup ---
            /* 74 */ 12, reg0_id, tempReg0,    // move %REG0 tempReg0
            /* 77 */ 12, reg1_id, tempReg1,    // move %REG1 tempReg1
            /* 80 */ 25, 0,                   // ldi 0  (REG0 <- 0)
            /* 82 */ 12, reg0_id, resultAddr,  // move %REG0 result (result <- 0)

            // --- Loop ---
            // loop: (início no endereço 85)
            /* 85 */ 11, yAddr, reg1_id,       // move y %REG1
            /* 88 */ 18, endLoopAddr,         // jz end_loop (se y==0, pula)
            // Corpo do loop
            /* 90 */ 11, resultAddr, reg0_id,   // move result %REG0
            /* 93 */ 1, xAddr, reg0_id,         // add x %REG0
            /* 96 */ 12, reg0_id, resultAddr,  // move %REG0 result
            // y--
            /* 99 */ 14, 1, reg0_id,            // move 1 %REG0
            /* 102 */ 4, reg0_id, reg1_id,      // sub %REG0 %REG1 (y = y-1)
            /* 105 */ 12, reg1_id, yAddr,       // move %REG1 y
            /* 108 */ 16, loopAddr,            // jmp loop
            
            // end_loop: (início no endereço 111)
            /* 111 */ 16, epilogueAddr         // jmp epilogueAddr (salta para o código dinâmico)
        };
        
        // Carrega a sub-rotina na memória
        int memoryAddress = 74;
        for (int value : subroutineMachineCode) {
            memory.getDataList()[memoryAddress++] = value;
        }
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

    //0
    // add %regA %regB => regB <- regA + regB
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

    //1
    // add <mem> %regA => regA <- MEM[mem] + regA
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

    //2
    // add %regA <mem> => MEM[mem] <- regA + MEM[mem]
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

    //3
    // add imm %regA => regA <- imm + regA
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

    //4
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

    //5
    // sub <mem> %regA => regA <- MEM[mem] - regA
    public void subMemReg() {
        incrementPC(); // PC aponta para o endereço <mem> (em N+1)

        PC.internalRead();      // intBus1 <- [PC]
        ula.internalStore(0);   // ula.reg1 <- [PC]
        ula.read(0);            // extBus <- [PC] (endereço do operando <mem>)
        memory.read();          // extBus <- MEM[[PC]] (o valor de <mem>, o endereço efetivo)
        // Neste ponto, extBus contém o endereço efetivo. Usamos ele para buscar o dado final.
        memory.read();          // extBus <- MEM[<mem>] (o valor final do primeiro operando)
        ula.store(0);           // ula.reg1 <- [valor de MEM[<mem>]]

        incrementPC();

        PC.internalRead();      // intBus1 <- [PC]
        ula.internalStore(1);   // ula.reg2 <- [PC]
        ula.read(1);            // extBus <- [PC] (endereço do ID de regA)
        memory.read();          // extBus <- ID de regA

        demux.setValue(extBus.get()); // demux <- ID de regA
        registersInternalRead();      // intBus1 <- [conteúdo de regA]
        ula.internalStore(1);         // ula.reg2 <- [conteúdo de regA]

        // subtrai e grava em regA
        ula.sub();
        setStatusFlags(intBus1.get());

        ula.internalRead(1);
        registersInternalStore();

        incrementPC();
    }

    //6
    // sub %regA <mem> => MEM[mem] <- regA - MEM[mem]
    public void subRegMem() {
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

        // subtrai e grava na memoria
        ula.sub();
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

    //7
    // sub imm %regA => regA <- imm - regA
    public void subImmReg() {
        // busca imediato
        incrementPC();

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

        // subtrai e grava em regA
        ula.sub();
        setStatusFlags(intBus1.get());

        ula.internalRead(1);
        registersInternalStore();

        incrementPC();
    }

    private void writeToMemory(int address, int data) {
        // Guarda o dado em ula.reg1
        intBus1.put(data);
        ula.internalStore(0);
        // Guarda o endereço em ula.reg2
        intBus1.put(address);
        ula.internalStore(1);
        // Realiza a escrita em 2 passos
        ula.read(1);        // Põe o endereço no extBus
        memory.store();     // Prepara a memória
        ula.read(0);        // Põe o dado no extBus
        memory.store();     // Escreve o dado
    }

    //8
    // imul <mem> %<RegA>           || RegA <- RegA x memória[mem] (produto de inteiros)
    public void imulMemReg() {
        // --- Definições ---
        int subroutineAddr = 74, returnAddr = 121, resultAddr = 123, xAddr = 125, yAddr = 124, epilogueAddr = 114;
        int pc_id = 5;

        // --- FASE 1: BUSCAR OPERANDO DA MEMÓRIA E PASSAR COMO PARÂMETRO 'X' ---
        incrementPC(); // PC aponta para ponteiro <mem>
        PC.internalRead(); ula.internalStore(0); ula.read(0); memory.read();
        memory.read(); ula.store(0); // ula.reg1 <- [MEM[mem]]
        // Escreve [MEM[mem]] em xAddr (125)
        intBus1.put(xAddr); ula.internalStore(1);
        ula.read(1); memory.store();
        ula.read(0); memory.store();

        // --- FASE 2: BUSCAR REG_A E PASSAR COMO PARÂMETRO 'Y' ---
        incrementPC(); // PC aponta para id_regA
        PC.internalRead(); ula.internalStore(0); ula.read(0); memory.read(); demux.setValue(extBus.get());
        registersInternalRead(); ula.internalStore(0); // ula.reg1 <- [regA]
        int destRegId = demux.getValue(); // Guarda o ID do registrador de destino
        // Escreve [regA] em yAddr (124)
        intBus1.put(yAddr); ula.internalStore(1);
        ula.read(1); memory.store();
        ula.read(0); memory.store();
        
        // --- FASE 3: PREPARAR O EPÍLOGO E O RETORNO ---
        // O destino é um registrador, o epílogo é mais simples:
        // 1. move resultAddr %REG_destino
        // 2. move returnAddr %PC

        // Escreve instrução 1: move resultAddr %regA
        writeToMemory(epilogueAddr, 11);
        writeToMemory(epilogueAddr + 1, resultAddr);
        writeToMemory(epilogueAddr + 2, destRegId); // Usa o ID do registrador de destino

        // Escreve instrução 2: move returnAddr %PC
        writeToMemory(epilogueAddr + 3, 11);
        writeToMemory(epilogueAddr + 4, returnAddr);
        writeToMemory(epilogueAddr + 5, pc_id);
        
        // Salva o endereço de retorno
        incrementPC(); // PC aponta para a próxima instrução
        PC.internalRead();
        writeToMemory(returnAddr, PC.getData());

        // --- FASE 4: SALTAR PARA A SUB-ROTINA ---
        intBus1.put(subroutineAddr);
        PC.internalStore();
    }

    //9
    //imul %<RegA> <mem>          || memória[mem] <- RegA x memória[mem] (idem)
    /**
     * imul %regA <mem> => memória[mem] <- RegA * memória[mem]
     * Versão final e correta, usando o padrão "Busca-e-Passa".
     */
    public void imulRegMem() {
        // --- Definições ---
        int subroutineAddr = 74, returnAddr = 121, resultAddr = 123, xAddr = 125, yAddr = 124, epilogueAddr = 114;
        int reg0_id = 1, pc_id = 5;

        // --- FASE 1: BUSCAR REG_A E PASSAR COMO PARÂMETRO 'X' ---
        incrementPC(); // PC aponta para id_regA
        PC.internalRead(); ula.internalStore(0); ula.read(0); memory.read(); demux.setValue(extBus.get());
        registersInternalRead(); ula.internalStore(0); // ula.reg1 <- [regA]
        // Escreve [regA] em xAddr (125)
        intBus1.put(xAddr); ula.internalStore(1); // ula.reg2 <- endereço 125
        ula.read(1); memory.store();              // Prepara a escrita na memória
        ula.read(0); memory.store();              // Escreve o dado [regA] em xAddr

        // --- FASE 2: BUSCAR OPERANDO DA MEMÓRIA, SALVAR SEU ENDEREÇO, E PASSAR O DADO COMO PARÂMETRO 'Y' ---
        incrementPC(); // PC aponta para ponteiro <mem>
        PC.internalRead(); ula.internalStore(0); ula.read(0); memory.read(); // extBus <- ponteiro_mem
        int memDestAddr = extBus.get(); // GUARDA O ENDEREÇO DE DESTINO FINAL
        memory.read(); // extBus <- [MEM[mem]]
        ula.store(0);  // ula.reg1 <- [MEM[mem]]
        // Escreve [MEM[mem]] em yAddr (124)
        intBus1.put(yAddr); ula.internalStore(1);
        ula.read(1); memory.store();
        ula.read(0); memory.store();

        // --- FASE 3: PREPARAR O EPÍLOGO E O RETORNO ---
        // O destino é um endereço de memória, então o epílogo precisa ser:
        // 1. move resultAddr %REG0  (resultado para registrador temporário)
        // 2. move %REG0 <mem_destino> (do temporário para a memória)
        // 3. move returnAddr %PC     (retorno)

        // Escreve instrução 1: move resultAddr %REG0
        writeToMemory(epilogueAddr++, 11);
        writeToMemory(epilogueAddr++, resultAddr);
        writeToMemory(epilogueAddr++, reg0_id);

        // Escreve instrução 2: move %REG0 <mem_destino>
        writeToMemory(epilogueAddr++, 12);
        writeToMemory(epilogueAddr++, reg0_id);
        writeToMemory(epilogueAddr++, memDestAddr); // Usa o endereço salvo!

        // Escreve instrução 3: move returnAddr %PC
        writeToMemory(epilogueAddr++, 11);
        writeToMemory(epilogueAddr++, returnAddr);
        writeToMemory(epilogueAddr, pc_id);
        
        // Salva o endereço de retorno
        incrementPC(); // PC aponta para a próxima instrução
        PC.internalRead();
        writeToMemory(returnAddr, PC.getData());

        // --- FASE 4: SALTAR PARA A SUB-ROTINA ---
        intBus1.put(subroutineAddr);
        PC.internalStore();
    }

    //10 
    //imul %<RegA> %<RegB>        || RegB <- RegA x RegB (idem)
    public void imulRegReg() {
        // --- Endereços e IDs constantes da sub-rotina ---
        int subroutineAddr = 74;
        int returnAddr = 121;
        int xAddr = 125;
        int yAddr = 124;
        int operandToModifyAddr = 112;

        // --- FASE 1: BUSCAR AMBOS OS OPERANDOS ---
        // 1.1: Busca o conteúdo de %regA e armazena em ula.reg1
        incrementPC();
        PC.internalRead(); ula.internalStore(0); ula.read(0); memory.read(); demux.setValue(extBus.get());
        registersInternalRead(); ula.internalStore(0); // ula.reg1 agora contém [regA]

        // 1.2: Busca o conteúdo de %regB e armazena em ula.reg2.
        // O valor em ula.reg1 permanece intacto. O ID de %regB fica no demux.
        incrementPC();
        PC.internalRead(); ula.internalStore(1); ula.read(1); memory.read(); demux.setValue(extBus.get());
        registersInternalRead(); ula.internalStore(1); // ula.reg2 agora contém [regB]

        // --- FASE 2: PASSAR PARÂMETROS PARA A SUB-ROTINA ---
        // Agora que temos [regA] em ula.reg1 e [regB] em ula.reg2, escrevemos ambos na memória.

        // 2.1: Escreve o valor de regA (de ula.reg1) no endereço xAddr (125)
        intBus1.put(xAddr);       // Põe o endereço de destino (125) no intBus1
        ula.internalStore(1);     // Guarda o endereço temporariamente em ula.reg2 (sobrescreve [regB] temporariamente)
        ula.read(1);              // Põe o endereço (125) no extBus
        memory.store();           // Prepara a memória
        ula.read(0);              // Põe o dado [regA] (de ula.reg1) no extBus
        memory.store();           // Escreve o dado

        // 2.2: Recarrega [regB] em ula.reg2 (já que foi usado para o endereço) e escreve em yAddr (124)
        demux.setValue(demux.getValue()); // Garante que demux ainda aponta para regB
        registersInternalRead();          // intBus1 <- [regB]
        ula.internalStore(1);             // Recarrega ula.reg2 com [regB]
        
        intBus1.put(yAddr);       // Põe o endereço de destino (124) no intBus1
        ula.internalStore(0);     // Guarda o endereço temporariamente em ula.reg1 (sobrescreve [regA])
        ula.read(0);              // Põe o endereço (124) no extBus
        memory.store();           // Prepara a memória
        ula.read(1);              // Põe o dado [regB] (de ula.reg2) no extBus
        memory.store();           // Escreve o dado

        // --- FASE 3: PREPARAR O RETORNO ---
        
        // 3.1: Modifica a sub-rotina para que o resultado volte para %regB.
        intBus1.put(operandToModifyAddr); ula.internalStore(0);
        ula.read(0); memory.store();
        intBus1.put(demux.getValue()); ula.internalStore(1);
        ula.read(1); memory.store();

        // 3.2: Salva o endereço de retorno.
        incrementPC();
        PC.internalRead(); ula.internalStore(0);
        intBus1.put(returnAddr); ula.internalStore(1);
        ula.read(1); memory.store();
        ula.read(0); memory.store();

        // --- FASE 4: SALTAR PARA A SUB-ROTINA ---
        intBus1.put(subroutineAddr);
        PC.internalStore();
    }

    //11
    //move <mem> %<regA>          || RegA <- memória[mem]
    public void moveMemReg() {

        // --- FASE 1: BUSCAR O PONTEIRO DA MEMÓRIA E ARMAZENAR EM ula(0) ---

        incrementPC();

        // 1.2: Busca o valor do ponteiro na memória e salva em ula(0)
        PC.internalRead();          // intBus1 <- [PC] (endereço do ponteiro)
        ula.internalStore(1);       // ula(1) <- [PC] (usa ula(1) para o endereço)
        ula.read(1);                // extBus <- [PC]
        memory.read();              // Memória lê o endereço e coloca o DADO (o ponteiro) no extBus.
        ula.store(0);               // ula(0) <- extBus. (O ponteiro está salvo em ula(0)).

        // --- FASE 2: IDENTIFICAR O REGISTRADOR DE DESTINO (%regA) ---

        // 2.1: Avança o PC para apontar para o ID de %regA (em [PC+2])
        incrementPC();

        // 2.2: Usa o padrão para buscar o ID de %regA e configurar o demux
        PC.internalRead();          // intBus1 <- [PC] (endereço do ID de regA)
        ula.internalStore(1);       // ula(1) <- [PC] (usa ula(1) para o endereço)
        ula.read(1);                // extBus <- [PC]
        memory.read();              // Memória lê o endereço e coloca o DADO (ID do regA) no extBus.


        demux.setValue(extBus.get()); // Demux é configurado com o ID para selecionar %regA.

        // --- FASE 3: BUSCAR O VALOR FINAL NA MEMÓRIA E ARMAZENÁ-LO EM %regA ---

        // 3.1: Usa o ponteiro salvo em ula(0) para ler o valor final da memória
        ula.read(0);                // ula(0) -> extBus. (Coloca o ponteiro no barramento externo).
        memory.read();              // Memória lê do ponteiro e coloca o valor final no extBus.

        // 3.2: Move o valor final para o registrador de destino
        ula.store(1);               // ula(1) <- extBus (move o valor final para a ula temporariamente).
        ula.internalRead(1);        // ula(1) -> intBus1 (coloca o valor final no barramento interno).
        registersInternalStore();   // Registrador de Destino (%regA) <- intBus1. Transferência concluída.

        // --- FASE 4: ATUALIZAR O PC PARA A PRÓXIMA INSTRUÇÃO (PC+3) ---
        incrementPC();
    }

    //12
    //move %<regA> <mem>          || memória[mem] <- RegA
    public void moveRegMem() {
        // 1. BUSCA O CONTEÚDO DO REGISTRADOR DE ORIGEM E GUARDA NA ULA
        incrementPC(); // PC aponta para o ID de %regA (em N+1)
        
        PC.internalRead();      // intBus1 <- [PC]
        ula.internalStore(0);   // ula.reg1 <- [PC]
        ula.read(0);            // extBus <- [PC] (endereço do ID de regA)
        memory.read();          // extBus <- ID de regA
        
        demux.setValue(extBus.get()); // demux <- ID de regA
        registersInternalRead();      // intBus1 <- [conteúdo de regA]
        ula.internalStore(0);         // ula.reg1 <- [conteúdo de regA] (Guarda o DADO a ser movido)

        // 2. BUSCA O ENDEREÇO DE MEMÓRIA DE DESTINO E GUARDA NA ULA
        incrementPC(); // PC aponta para o endereço <mem> (em N+2)

        PC.internalRead();      // intBus1 <- [PC]
        ula.internalStore(1);   // ula.reg2 <- [PC]
        ula.read(1);            // extBus <- [PC]
        memory.read();          // extBus <- <mem> (o endereço de destino)
        ula.store(1);           // ula.reg2 <- <mem> (Guarda o ENDEREÇO de destino)

        // 3. REALIZA A ESCRITA NA MEMÓRIA
        // O endereço de destino está em ula.reg2 e o dado está em ula.reg1.
        // A escrita na memória é um processo de 2 passos.

        // Passo 1: Enviar o endereço de destino para a memória.
        ula.read(1);            // extBus <- [endereço de destino] (lido de ula.reg2)
        memory.store();         // A memória "prepara" a escrita no endereço recebido.

        // Passo 2: Enviar o dado a ser escrito.
        ula.read(0);            // extBus <- [dado] (lido de ula.reg1)
        memory.store();         // A memória escreve o dado no endereço preparado.

        // 4. INCREMENTA O PC PARA A PRÓXIMA INSTRUÇÃO
        incrementPC();
    }

    //13
    //move %<regA> %<regB>        || RegB <- RegA
    public void moveRegReg() {

        // --- FASE 1: BUSCAR O VALOR DO REGISTRADOR DE ORIGEM (%regA) E ARMAZENAR EM ula(0) ---

        // 1.1: Avança o PC para apontar para o ID de %regA (em [PC+1])
        incrementPC();

        // 1.2: Usa o padrão para buscar o ID de %regA, configurar o demux e ler seu valor
        PC.internalRead();          // intBus1 <- [PC] (endereço do ID de regA)
        ula.internalStore(0);       // ula(0) <- [PC] (usa ula(0) temporariamente para o endereço)
        ula.read(0);                // extBus <- [PC]
        memory.read();              // extBus <- ID de %regA (lido da memória)
        demux.setValue(extBus.get()); // Demux é configurado para %regA.
        registersInternalRead();    // intBus1 <- [conteúdo de %regA].

        // 1.3: Salva o valor de %regA em ula(0) para uso posterior
        ula.internalStore(0);       // ula(0) <- [conteúdo de %regA]. O valor está seguro.

        // --- FASE 2: IDENTIFICAR O REGISTRADOR DE DESTINO (%regB) E ESCREVER O VALOR ---

        // 2.1: Avança o PC para apontar para o ID de %regB (em [PC+2])
        incrementPC();

        // 2.2: Usa o padrão para buscar o ID de %regB e configurar o demux
        PC.internalRead();          // intBus1 <- [PC] (endereço do ID de regB)
        ula.internalStore(1);       // ula(1) <- [PC] (usa ula(1) para não sobrescrever o valor em ula(0))
        ula.read(1);                // extBus <- [PC]
        memory.read();              // extBus <- ID de %regB (lido da memória)
        demux.setValue(extBus.get()); // Demux é RECONFIGURADO para %regB.

        // 2.3: Coloca o valor salvo de %regA no barramento e armazena em %regB
        ula.internalRead(0);        // intBus1 <- [conteúdo de %regA] (lido de ula(0)).
        registersInternalStore();   // Registrador de Destino (%regB) <- intBus1. A cópia foi concluída.

        // --- FASE 3: ATUALIZAR O PC PARA A PRÓXIMA INSTRUÇÃO (PC+3) ---
        incrementPC();
    }

    //14
    //move imm %<regA>            || RegA <- immediate
    public void moveImmReg() {
        // 1. BUSCA O VALOR IMEDIATO E O ARMAZENA TEMPORARIAMENTE NA ULA
        incrementPC(); // PC aponta para o valor imediato (em N+1)
        
        PC.internalRead();      // intBus1 <- [PC]
        ula.internalStore(0);   // ula.reg1 <- [PC]
        ula.read(0);            // extBus <- [PC] (endereço do valor imediato)
        memory.read();          // extBus <- [valor imediato]
        ula.store(0);           // ula.reg1 <- [valor imediato] (Guarda o valor na ULA)

        // 2. BUSCA O ID DO REGISTRADOR DE DESTINO
        incrementPC(); // PC aponta para o ID de %regA (em N+2)

        PC.internalRead();
        ula.internalStore(1);
        ula.read(1);
        memory.read(); // extBus <- ID de regA
        
        demux.setValue(extBus.get()); // demux <- ID de regA

        // 3. MOVE O VALOR DA ULA PARA O REGISTRADOR DE DESTINO
        ula.internalRead(0);      // intBus1 <- [valor imediato] (lido de ula.reg1)
        registersInternalStore(); // %regA <- intBus1

        // 4. INCREMENTA PC PARA A PRÓXIMA INSTRUÇÃO
        incrementPC();
    }

    //15
    //inc %<regA>                 || RegA ++
    public void incReg() {
        incrementPC();

        ula.read(0); //regA ainda está na ula
        memory.read(); // retorna ID de regA

        demux.setValue(extBus.get());
        registersInternalRead();

        ula.internalStore(0);
        ula.inc();
        ula.internalRead(0);
        registersInternalStore();

        incrementPC();
    }
    
    //16
    //jmp <mem>                   || PC <- mem (desvio incondicional)
    /**
     * jmp <endereço> => PC <- <endereço>
     * Salta a execução do programa para o endereço especificado.
     */
    public void jmp() {
        // --- FASE 1: BUSCAR O ENDEREÇO DE DESTINO DA MEMÓRIA ---

        // 1.1: Avança o PC para apontar para o operando <endereço>.
        incrementPC(); // PC agora aponta para N+1.

        // 1.2: Busca o valor do operando <endereço> que está na memória.
        // O caminho precisa ser: PC -> intBus1 -> ULA -> extBus -> Memória.
        PC.internalRead();      // intBus1 <- [PC] (que agora é N+1).
        ula.internalStore(0);   // ula.reg1 <- [PC] (Usa a ULA como ponte).
        ula.read(0);            // extBus <- [PC] (Coloca o endereço do operando no barramento externo).
        memory.read();          // Memória lê o endereço e coloca o DADO (o endereço de destino) no extBus.

        // Neste ponto, o endereço para o qual queremos saltar está no extBus.

        // --- FASE 2: CARREGAR O ENDEREÇO DE DESTINO NO PC ---

        // 2.1: Move o endereço de destino do extBus para o intBus, usando a ULA como ponte.
        ula.store(0);           // ula.reg1 <- extBus (ULA captura o endereço de destino).
        ula.internalRead(0);    // ula.reg1 -> intBus1 (ULA joga o endereço de destino no barramento interno).

        // 2.2: O PC armazena o valor que está no barramento interno.
        PC.internalStore();     // PC <- intBus1. O salto (JUMP) foi concluído.

        // Não há um terceiro incremento de PC. A próxima instrução a ser buscada (fetch)
        // já usará o novo valor do PC.
    }

    //17
    //jn <mem>                    || se última operação<0 então PC <- mem (desvio condicional)
    /**
     * jn <mem> => se Flags.Negative == 1, então PC <- mem
     * Desvia a execução para o endereço <mem> se a flag Negativo estiver ativa (1),
     * o que geralmente ocorre após uma operação de subtração resultar em um
     * número negativo.
     */
    public void jn() {

        // --- FASE 1: PREPARAÇÃO - CARREGAR OS DOIS POSSÍVEIS ENDEREÇOS NA statusMemory ---

        // 1.1: Calcula e armazena o endereço de "continuação" (PC+2) em statusMemory[0].
        // Este é o endereço para onde o PC irá se a condição de pulo for falsa.
        PC.internalRead();          // PC -> intBus1
        ula.internalStore(1);       // ula(1) <- PC (Usa ula(1) pois o método inc() opera sobre ele)
        ula.inc();                  // ula(1)++ (PC+1)
        ula.inc();                  // ula(1)++ (PC+2)
        ula.read(1);                // ula(1) -> extBus (Endereço PC+2 está no barramento externo)
        statusMemory.storeIn0();    // statusMemory[0] <- extBus. Endereço de continuação salvo.

        // 1.2: Busca o endereço de "pulo" de [PC+1] e armazena em statusMemory[1].
        // Este é o endereço para onde o PC irá se a condição de pulo for verdadeira.
        PC.internalRead();          // PC -> intBus1
        ula.internalStore(1);       // ula(1) <- PC
        ula.inc();                  // ula(1)++ (PC+1)
        ula.read(1);                // ula(1) -> extBus (Endereço do operando está no extBus)
        memory.read();              // Memória lê de [PC+1] e coloca o endereço de pulo no extBus.
        statusMemory.storeIn1();    // statusMemory[1] <- extBus. Endereço de pulo salvo.


        // --- FASE 2: SELEÇÃO - USAR A FLAG NEGATIVO PARA LER O ENDEREÇO CORRETO E ATUALIZAR O PC ---

        // 2.1: Coloca o valor da flag Negativo (0 ou 1) no barramento externo.
        extBus.put(Flags.getBit(1)); // << PONTO CHAVE: Usa o bit 1 (flag Negativo) como seletor.

        // 2.2: Usa o valor da flag no barramento como endereço para ler da statusMemory.
        statusMemory.read();        // Se extBus=0, lê de statusMemory[0]. Se extBus=1, lê de statusMemory[1].
                                    // O endereço de destino correto está agora no extBus.

        // 2.3: Move o endereço selecionado do extBus para o PC (via ULA).
        ula.store(0);               // ula(0) <- extBus (ULA captura o endereço correto).
        ula.internalRead(0);        // ula(0) -> intBus1 (ULA o joga para o barramento interno).
        PC.internalStore();         // PC <- intBus1. O PC é atualizado, finalizando o desvio.
    }

    //18
    // jz <mem>                    || se última operação=0 então PC <- mem (desvio condicional)
    /* jz <mem> => se Flags.Zero == 1, então PC <- mem
    * Desvia a execução para o endereço <mem> se a flag Zero estiver ativa (1).
    * Utiliza uma "statusMemory" para realizar a seleção condicional do próximo PC.
    */
    public void jz() {

        // --- FASE 1: PREPARAÇÃO - CARREGAR OS DOIS POSSÍVEIS ENDEREÇOS NA statusMemory ---

        // 1.1: Calcula e armazena o endereço de "continuação" (PC+2) em statusMemory[0].
        PC.internalRead();          // PC -> intBus1
        ula.internalStore(0);       // ula(0) <- PC (Guarda o PC original para cálculos)
        ula.inc();                  // ula(0)++ (PC+1)
        ula.inc();                  // ula(0)++ (PC+2)
        ula.read(0);                // ula(0) -> extBus (Endereço PC+2 está no barramento externo)
        statusMemory.storeIn0();    // statusMemory[0] <- extBus. Endereço de continuação salvo.

        // 1.2: Busca o endereço de "pulo" de [PC+1] e armazena em statusMemory[1].
        PC.internalRead();          // PC -> intBus1
        ula.internalStore(0);       // ula(0) <- PC
        ula.inc();                  // ula(0)++ (PC+1)
        ula.read(0);                // ula(0) -> extBus (Endereço do operando está no extBus)
        memory.read();              // Memória lê de [PC+1] e coloca o endereço de pulo no extBus.
        statusMemory.storeIn1();    // statusMemory[1] <- extBus. Endereço de pulo salvo.


        // --- FASE 2: SELEÇÃO - USAR A FLAG ZERO PARA LER O ENDEREÇO CORRETO E ATUALIZAR O PC ---

        // 2.1: Coloca o valor da flag Zero (0 ou 1) no barramento externo.
        extBus.put(Flags.getBit(0)); // Flags.getBit(0) retorna o estado da flag Zero.

        // 2.2: Usa o valor da flag no barramento como endereço para ler da statusMemory.
        statusMemory.read();        // Se extBus=0, lê de statusMemory[0]. Se extBus=1, lê de statusMemory[1].
                                    // O endereço de destino (pulo ou continuação) está agora no extBus.

        // 2.3: Move o endereço selecionado do extBus para o PC (via ULA).
        ula.store(0);               // ula(0) <- extBus (ULA captura o endereço correto).
        ula.internalRead(0);        // ula(0) -> intBus1 (ULA o joga para o barramento interno).
        PC.internalStore();         // PC <- intBus1. O PC é atualizado com o destino correto.
    }

    //19
    //jeq %<regA> %<regB> <mem>   || se RegA==RegB então PC <- mem (desvio condicional)
    public void jeq() {
        // --- FASE 1: BUSCAR OPERANDOS E REALIZAR COMPARAÇÃO ---
        // Busca regA e armazena em ula.reg1
        incrementPC();
        PC.internalRead(); 
        ula.internalStore(0); 
        ula.read(0); 
        memory.read(); 
        demux.setValue(extBus.get());
        registersInternalRead(); 
        ula.internalStore(0);

        // Busca regB e armazena em ula.reg2
        incrementPC();
        PC.internalRead(); 
        ula.internalStore(1); 
        ula.read(1); 
        memory.read(); 
        demux.setValue(extBus.get());
        registersInternalRead(); 
        ula.internalStore(1);

        // Compara regA - regB e atualiza as flags
        ula.sub();
        setStatusFlags(intBus1.get()); // Z=1 se regA == regB

        // --- FASE 2: PREPARAR ENDEREÇOS E EXECUTAR DESVIO CONDICIONAL ---
        // O PC está em N+2. O endereço de pulo está em N+3 e o de continuação é N+4.

        // Endereço de "continuação" (N+4):
        PC.internalRead(); 
        ula.internalStore(0); 
        ula.inc(); 
        ula.inc(); 
        ula.read(0);
        statusMemory.storeIn0(); // statusMemory[0] <- Endereço para onde NÃO pular.

        // Endereço de "pulo" (<mem> de N+3):
        incrementPC();
        PC.internalRead(); 
        ula.internalStore(1); 
        ula.read(1); 
        memory.read();
        statusMemory.storeIn1(); // statusMemory[1] <- Endereço para onde PULAR.

        // Usa a flag Zero (Z) como seletor. Se Z=1 (iguais), lê statusMemory[1].
        extBus.put(Flags.getBit(0));
        statusMemory.read();
        
        // Carrega o endereço selecionado no PC
        ula.store(0);
        ula.internalRead(0);
        PC.internalStore();
    }

    //20
    // jneq %<regA> %<regB> <mem>  || se RegA!=RegB então PC <- mem (desvio condicional)
    public void jneq() {
        // --- FASE 1: BUSCAR OPERANDOS E REALIZAR COMPARAÇÃO
        incrementPC();
        PC.internalRead(); 
        ula.internalStore(0); 
        ula.read(0); 
        memory.read(); 
        demux.setValue(extBus.get());
        registersInternalRead(); 
        ula.internalStore(0);

        incrementPC();
        PC.internalRead(); 
        ula.internalStore(1); 
        ula.read(1); 
        memory.read(); 
        demux.setValue(extBus.get());
        registersInternalRead(); 
        ula.internalStore(1);

        ula.sub();
        setStatusFlags(intBus1.get()); // Z=0 se regA != regB

        // --- FASE 2: PREPARAR ENDEREÇOS E EXECUTAR DESVIO CONDICIONAL ---
        // O PC está em N+2. O endereço de pulo está em N+3 e o de continuação é N+4.

        // Endereço de "pulo" (<mem> de N+3): **ARMAZENADO EM statusMemory[0]**
        incrementPC();
        PC.internalRead(); 
        ula.internalStore(0); 
        ula.read(0); 
        memory.read();
        statusMemory.storeIn0(); // << LÓGICA INVERTIDA: Endereço de PULO vai para a posição 0.

        // Endereço de "continuação" (N+4): **ARMAZENADO EM statusMemory[1]**
        // (PC foi para N+3, então PC+1 = N+4)
        PC.internalRead(); 
        ula.internalStore(1); 
        ula.inc(); 
        ula.read(1);
        statusMemory.storeIn1(); // << LÓGICA INVERTIDA: Endereço de CONTINUAÇÃO vai para a posição 1.

        // Usa a flag Zero (Z) como seletor. Se Z=0 (diferentes), lê statusMemory[0] (pulo).
        extBus.put(Flags.getBit(0));
        statusMemory.read();
        
        // Carrega o endereço selecionado no PC
        ula.store(0);
        ula.internalRead(0);
        PC.internalStore();
    }

    //21
    // jgt %<regA> %<regB> <mem>   || se RegA>RegB então PC <- mem (desvio condicional)
    public void jgt() {
        // --- FASE 1: BUSCAR OPERANDOS E REALIZAR COMPARAÇÃO (regA - regB) ---

        // 1.1: Busca o valor de %regA e armazena em ula.reg1.
        incrementPC(); // PC aponta para N+1 (ID de regA).
        PC.internalRead(); 
        ula.internalStore(0); 
        ula.read(0); 
        memory.read(); 
        demux.setValue(extBus.get());
        registersInternalRead(); 
        ula.internalStore(0); // ula.reg1 <- [conteúdo de regA]

        // 1.2: Busca o valor de %regB e armazena em ula.reg2.
        incrementPC(); // PC aponta para N+2 (ID de regB).
        PC.internalRead(); 
        ula.internalStore(1); 
        ula.read(1); 
        memory.read(); 
        demux.setValue(extBus.get());
        registersInternalRead(); 
        ula.internalStore(1); // ula.reg2 <- [conteúdo de regB]

        // 1.3: ULA executa regA - regB e atualiza as flags. **LÓGICA CORRIGIDA**
        ula.sub(); // Calcula ula.reg1 - ula.reg2 (regA - regB) diretamente.
        setStatusFlags(intBus1.get()); // As flags N e Z originais (da subtração) estão prontas.


        // --- FASE 2: PREPARAÇÃO DOS ENDEREÇOS DE DESVIO ---
        // O PC está em N+2. O endereço de pulo está em [N+3] e o de continuação é N+4.

        // Endereço de "continuação" (N+4):
        PC.internalRead(); 
        ula.internalStore(0); 
        ula.inc(); 
        ula.inc(); 
        ula.read(0);
        statusMemory.storeIn0(); // statusMemory[0] <- N+4

        // Endereço de "pulo" (de [N+3]):
        incrementPC(); // PC aponta para N+3 (endereço <mem>).
        PC.internalRead(); 
        ula.internalStore(1); 
        ula.read(1); 
        memory.read();
        statusMemory.storeIn1(); // statusMemory[1] <- <mem>


        // --- FASE 3: COMBINAÇÃO DAS FLAGS E DESVIO FINAL ---
        // Esta fase já estava logicamente correta e é mantida. Agora ela recebe as flags corretas.

        // 3.1: Carrega as flags N e Z na ULA.
        extBus.put(Flags.getBit(1)); // Põe a flag Negativo (N) no extBus.
        ula.store(0);                // ula.reg1 <- N

        extBus.put(Flags.getBit(0)); // Põe a flag Zero (Z) no extBus.
        ula.store(1);                // ula.reg2 <- Z

        // 3.2: Soma as flags. A nova flag Zero será 1 se e somente se (N+Z) for 0.
        ula.add();
        setStatusFlags(intBus1.get()); // Flags (N', Z') agora refletem o estado de (N+Z).

        // 3.3: Usa a NOVA flag Zero (Z') como seletor.
        // Se regA > regB, então N=0 e Z=0. A soma N+Z=0, e a nova flag Zero (Z') será 1.
        // Usamos Z' para selecionar statusMemory[1] (o endereço de pulo).
        // Se regA <= regB, então N=1 ou Z=1. A soma N+Z != 0, e Z' será 0.
        // Usamos Z' para selecionar statusMemory[0] (o endereço de continuação).
        extBus.put(Flags.getBit(0));
        statusMemory.read();
        
        // 3.4: Move o endereço selecionado para o PC.
        ula.store(0);
        ula.internalRead(0);
        PC.internalStore();
    }

    //22
    //jlw %<regA> %<regB> <mem>   || se RegA<RegB então PC <- mem (desvio condicional)
    public void jlw() {
        // --- FASE 1: BUSCAR OPERANDOS E REALIZAR COMPARAÇÃO (regA - regB) ---

        // 1.1: Busca o valor de %regA e armazena em ula.reg1.
        incrementPC(); // PC aponta para N+1 (ID de regA).
        PC.internalRead(); 
        ula.internalStore(0); 
        ula.read(0); 
        memory.read(); 
        demux.setValue(extBus.get());
        registersInternalRead(); 
        ula.internalStore(0); // ula.reg1 <- [conteúdo de regA]

        // 1.2: Busca o valor de %regB e armazena em ula.reg2.
        incrementPC(); // PC aponta para N+2 (ID de regB).
        PC.internalRead(); 
        ula.internalStore(1); 
        ula.read(1); 
        memory.read(); 
        demux.setValue(extBus.get());
        registersInternalRead(); 
        ula.internalStore(1); // ula.reg2 <- [conteúdo de regB]

        // 1.3: ULA executa regA - regB (ula.reg1 - ula.reg2) e atualiza as flags.
        ula.sub();
        setStatusFlags(intBus1.get()); // Flags são atualizadas com base no resultado.

        // --- FASE 2: DESVIO CONDICIONAL (BASEADO NA FLAG NEGATIVO) ---

        // 2.1: Prepara os endereços de "pulo" e "continuação" na statusMemory.
        // O PC está em N+2. O endereço de "pulo" está em N+3 e o de "continuação" é N+4.

        // Endereço de "continuação" (N+4, ou seja, PC+2):
        PC.internalRead(); 
        ula.internalStore(0); 
        ula.inc(); 
        ula.inc(); 
        ula.read(0);
        statusMemory.storeIn0(); // statusMemory[0] <- PC+2 (N+4)

        // Endereço de "pulo" (de [N+3]):
        incrementPC(); // PC aponta para N+3 (endereço <mem>).
        PC.internalRead(); 
        ula.internalStore(1); 
        ula.read(1); 
        memory.read();
        statusMemory.storeIn1(); // statusMemory[1] <- <mem>

        // 2.2: Usa a flag Negativo (bit 1) para selecionar o próximo PC.
        extBus.put(Flags.getBit(1));
        statusMemory.read(); // O endereço de destino correto (pulo ou continuação) está agora no extBus.
        
        // 2.3: Move o endereço selecionado para o PC.
        ula.store(0);
        ula.internalRead(0);
        PC.internalStore(); // PC é atualizado com o destino correto.
    }

    //23
    //read
    /**
     * read <mem> => REG0 <- Memória[Memória[PC+1]]
     * Realiza uma leitura indireta da memória. O operando <mem> na posição [PC+1]
     * é um ponteiro para o endereço final de onde o dado será lido. O valor lido
     * é armazenado em REG0.
     */
    public void read() {
        // --- FASE 1: BUSCAR O VALOR DO PONTEIRO (Correto no original) ---
        incrementPC(); // PC aponta para N+1

        PC.internalRead();
        ula.internalStore(1);
        ula.read(1);
        memory.read();
        ula.store(0); // O valor do ponteiro está salvo em ula.reg1

        // --- FASE 2: USAR O PONTEIRO PARA BUSCAR O DADO FINAL E ARMAZENAR EM REG0 ---
        ula.read(0);      // Coloca o ponteiro no extBus
        memory.read();    // O dado final está agora no extBus

        ula.store(1);     // ula.reg2 captura o dado final do extBus
        ula.internalRead(1);  // ula.reg2 coloca o dado final no intBus1

        // AJUSTE 1: Usando o demux para manter o padrão arquitetural
        demux.setValue(1);        // Demux agora aponta para REG0 (índice 1)
        registersInternalStore(); // REG0 armazena o valor do intBus1

        // --- FASE 3: ATUALIZAR O PC PARA A PRÓXIMA INSTRUÇÃO ---
        // A instrução usou 1 operando, então o PC avança mais uma vez para N+2
        incrementPC();
    }

    //24
    //store
    public void store() {
        // --- FASE 1: BUSCAR O ENDEREÇO DE DESTINO ---
        incrementPC(); // PC aponta para o operando <mem> (em N+1)
        
        PC.internalRead();
        ula.internalStore(1);
        ula.read(1);
        memory.read(); // O endereço de destino <mem> está no extBus
        ula.store(1);  // ula.reg2 <- <mem> (Guarda o ENDEREÇO de destino)

        // --- FASE 2: BUSCAR O DADO DE REG0 ---
        // AJUSTE: Usando o demux para manter o padrão
        demux.setValue(1);        // Aponta para REG0 (índice 1)
        registersInternalRead();  // intBus1 <- [conteúdo de REG0]
        ula.internalStore(0);     // ula.reg1 <- [conteúdo de REG0] (Guarda o DADO)

        // --- FASE 3: REALIZAR O ARMAZENAMENTO NA MEMÓRIA ---
        // Esta parte já estava perfeita no seu código original
        ula.read(1);      // Envia o ENDEREÇO de destino para o extBus
        memory.store();   // 1º passo: Memória captura o endereço

        ula.read(0);      // Envia o DADO para o extBus
        memory.store();   // 2º passo: Memória escreve o dado no endereço

        // --- FASE 4: ATUALIZAR O PC PARA A PRÓXIMA INSTRUÇÃO ---
        // O PC já avançou uma vez, agora avança de novo para N+2
        incrementPC();
    }

    //25
    //ldi 
    /**
     * ldi <immediate> => REG0 <- immediate
     * Versão final corrigida, respeitando que nem o PC nem o REG0 acessam
     * o barramento externo diretamente. A ULA é usada como ponte para
     * endereços e dados.
     */
    public void ldi() {

        // --- FASE 1: LEVAR O ENDEREÇO [PC+1] PARA O EXTBUS VIA ULA ---

        // 1.1: Calcula o endereço do imediato (PC+1) e o armazena em ula(1).
        PC.internalRead();          // PC -> intBus1
        ula.internalStore(1);       // ula(1) <- intBus1 (ula(1) guarda o valor original do PC).
        ula.inc();                  // ula(1)++ (ula(1) agora tem o endereço PC+1).

        // 1.2: Usa a ULA para colocar o endereço no barramento externo.
        ula.read(1);                // ula(1) -> extBus.


        // --- FASE 2: BUSCAR O IMEDIATO E MOVER PARA REG0 VIA ULA ---

        // 2.1: Memória lê o endereço e coloca o dado (imediato) no extBus.
        memory.read();              // extBus <- Memória[PC+1].

        // 2.2: ULA busca o valor do extBus e o move para o intBus.
        ula.store(1);               // ula(1) <- extBus (ULA captura o valor imediato).
        ula.internalRead(1);        // ula(1) -> intBus1 (ULA coloca o valor imediato no barramento interno).

        // 2.3: REG0 armazena o valor que está no intBus.
        demux.setValue(1);        // Aponta para REG0 (índice 1)
        registersInternalStore();

        // --- FASE 3: ATUALIZAR O PC PARA A PRÓXIMA INSTRUÇÃO ---
        // Precisamos recalcular PC+2, pois ula(1) foi sobrescrito.
        PC.internalRead();          // PC -> intBus1.
        ula.internalStore(1);       // ula(1) <- intBus1.
        ula.inc();                  // ula(1)++ (PC+1).
        ula.inc();                  // ula(1)++ (PC+2).
        ula.internalRead(1);        // ula(1) -> intBus1.
        PC.internalStore();         // PC <- intBus1. Fim do microprograma.
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
			imulMemReg(); 
			break;

		case 9:
			imulRegMem(); 
			break;

		case 10:
			imulRegReg();  
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






