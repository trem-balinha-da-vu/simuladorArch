package architecture;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

import components.*;
import components.ULA;

public class Architecture {
    private boolean simulation;

    private boolean halt;

    private Bus extBus;
    private Bus intBus1;
    private Bus intBus2;

    private Memory memory;

    private Memory statusMemory;

    private int memorySize;

    private Register PC;
    private Register IR;
    private Register REG0;
    private Register REG1;
    private Register REG2;
    private Register REG3;
    private Register StkTOP;
    private Register StkBOT;
    private Register Flags;
    private ULA ula;
    private Demux demux;

    private ArrayList<String> commandsList;
    private ArrayList<Register> registersList;

    private void componentsInstances() {
        // buses -> registers -> ula -> memory
        extBus = new Bus();

        IR = new Register("IR", extBus, intBus2);
        REG0 = new Register("RPG0", extBus, intBus1);
        REG1 = new Register("RPG1", extBus, intBus1);
        REG2 = new Register("RPG2", extBus, intBus1);
        REG3 = new Register("RPG3", extBus, intBus1);
        PC = new Register("PC", extBus, intBus2);
        // adicionar o stktop e o stkbot?
        Flags = new Register(2, intBus2);
        fillRegistersList();

        ula = new ULA(extBus);
        statusMemory = new Memory(2, extBus);
        memorySize = 128;
        memory = new Memory(memorySize, extBus);
        demux = new Demux();

        fillCommandsList();
    }


    // preenche a lista de registradores
    private void fillRegistersList() {
        registersList = new ArrayList<Register>();
        registersList.add(IR);
        registersList.add(REG0);
        registersList.add(REG1);
        registersList.add(REG2);
        registersList.add(REG3);
        registersList.add(PC);
        registersList.add(StkTOP);
        registersList.add(StkBOT);
        registersList.add(Flags);
    }

    // o construtor que instancia todos os componentes de
    // acordo com o diagrama da arquitetura
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

    // getters
    protected Bus getExtBus() { return extBus; }
    protected Bus getIntBus1() { return intBus1; }
    protected Bus getIntBus2() { return intBus2; }

    protected Memory getMemory() { return memory; }

    protected Register getPC() { return PC; }
    protected Register getIR() { return IR; }

    // metodo geral para registradores
    // eles so retornam listas, e nao alteram o estado diretamente
    // entao ta safe
    public ArrayList<Register> getRegistersList() {
        return registersList;
    }
    protected Register getFlags() { return Flags; }
    protected ULA getUla() { return ula; }

    public ArrayList<String> getCommandsList() {
        return commandsList;
    }

    private void registersRead() {
        int regId = demux.getValue();
        if (regId >= 0 && regId < registersList.size()) {
            registersList.get(regId).read();
        }
    }

    private void registersStore() {
        int regId = demux.getValue();
        if (regId >= 0 && regId < registersList.size()) {
            registersList.get(regId).store();
        }
    }

    // colocar os microprogramas entre eles
    //the instructions table is
    // CONFERIR O Microprogramas.txt
	/*
	 *
			add addr (rpg <- rpg + addr)
			sub addr (rpg <- rpg - addr)
			jmp addr (pc <- addr)
			jz addr  (se bitZero pc <- addr)
			jn addr  (se bitneg pc <- addr)
			read addr (rpg <- addr)
			store addr  (addr <- rpg)
			ldi x    (rpg <- x. x must be an integer)
			inc    (rpg++)
			move regA regB (regA <- regB)
	 */

    // sub %regA %regB => regB <- regA - regB
    public void subRegReg() {
        // busca regA
        PC.internalRead();
        ula.internalStore(1);
        ula.inc();
        ula.internalRead(1);
        PC.internalStore();

        ula.read(1);
        memory.read();
        ula.read(1);
        memory.read();
        ula.read(1);
        demux.setValue(extBus.getData());
        registersRead();
        ula.internalStore(0);

        // busca regB
        PC.internalRead();
        ula.internalStore(1);
        ula.inc();
        ula.internalRead(1);
        PC.internalStore();

        ula.read(1);
        memory.read();
        ula.read(1);
        memory.read();
        ula.read(1);
        demux.setValue(extBus.getData());
        registersRead();
        ula.internalStore(1);

        // subtrai e grava em regB
        ula.sub();
        setStatusFlags(intBus1.getData());

        ula.internalRead(1);
        registersStore();

        PC.internalRead();
        ula.internalStore(1);
        ula.inc();
        ula.internalRead(1);
        PC.internalStore();
    }

    // sub <mem> %regA => regA <- MEM[mem] - regA
    public void subMemReg() {
        // busca endereco da memoria
        PC.internalRead();
        ula.internalStore(1);
        ula.inc();
        ula.internalRead(1);
        PC.internalStore();

        ula.read(1);
        memory.read();
        memory.read();
        ula.store(0);

        // busca regA
        PC.internalRead();
        ula.internalStore(1);
        ula.inc();
        ula.internalRead(1);
        PC.internalStore();

        ula.read(1);
        memory.read();
        ula.read(1);
        memory.read();
        ula.read(1);
        demux.setValue(extBus.getData());
        registersRead();
        ula.internalStore(1);

        // subtrai e grava em regA
        ula.sub();
        setStatusFlags(intBus1.getData());

        ula.internalRead(1);
        registersStore();

        PC.internalRead();
        ula.internalStore(1);
        ula.inc();
        ula.internalRead(1);
        PC.internalStore();
    }

    // sub %regA <mem> => MEM[mem] <- regA - MEM[mem]
    public void subRegMem() {
        // busca regA
        PC.internalRead();
        ula.internalStore(1);
        ula.inc();
        ula.internalRead(1);
        PC.internalStore();

        ula.read(1);
        memory.read();
        ula.read(1);
        memory.read();
        ula.read(1);
        demux.setValue(extBus.getData());
        registersRead();
        ula.internalStore(0);

        // busca endereco de memoria
        PC.internalRead();
        ula.internalStore(1);
        ula.inc();
        ula.internalRead(1);
        PC.internalStore();

        ula.read(1);
        memory.read();
        memory.read();
        ula.store(1);

        // subtrai e grava na memoria
        ula.sub();
        setStatusFlags(intBus1.getData());

        ula.internalRead(1);
        memory.store();
        memory.store();

        PC.internalRead();
        ula.internalStore(1);
        ula.inc();
        ula.internalRead(1);
        PC.internalStore();
    }

    // sub imm %regA => regA <- imm - regA
    public void subImmReg() {
        // busca imediato
        PC.internalRead();
        ula.internalStore(1);
        ula.inc();
        ula.internalRead(1);
        PC.internalStore();

        ula.read(1);
        memory.read();
        ula.store(0);

        // busca regA
        PC.internalRead();
        ula.internalStore(1);
        ula.inc();
        ula.internalRead(1);
        PC.internalStore();

        ula.read(1);
        memory.read();
        ula.read(1);
        memory.read();
        ula.read(1);
        demux.setValue(extBus.getData());
        registersRead();
        ula.internalStore(1);

        // subtrai e grava em regA
        ula.sub();
        setStatusFlags(intBus1.getData());

        ula.internalRead(1);
        registersStore();

        PC.internalRead();
        ula.internalStore(1);
        ula.inc();
        ula.internalRead(1);
        PC.internalStore();
    }

    public void moveMemReg() {

        // --- FASE 1: BUSCAR O PONTEIRO DA MEMÓRIA E ARMAZENAR EM ula(0) ---

        // 1.1: Avança o PC para apontar para o endereço do ponteiro (em [PC+1])
        PC.internalRead();
        ula.internalStore(1);
        ula.inc();
        PC.internalStore();

        // 1.2: Busca o valor do ponteiro na memória e salva em ula(0)
        PC.internalRead();          // intBus1 <- [PC] (endereço do ponteiro)
        ula.internalStore(1);       // ula(1) <- [PC] (usa ula(1) para o endereço)
        ula.read(1);                // extBus <- [PC]
        memory.read();              // Memória lê o endereço e coloca o DADO (o ponteiro) no extBus.
        ula.store(0);               // ula(0) <- extBus. (O ponteiro está salvo em ula(0)).

        // --- FASE 2: IDENTIFICAR O REGISTRADOR DE DESTINO (%regA) ---

        // 2.1: Avança o PC para apontar para o ID de %regA (em [PC+2])
        PC.internalRead();
        ula.internalStore(1);
        ula.inc();
        PC.internalStore();

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
        PC.internalRead();
        ula.internalStore(1);
        ula.inc();
        PC.internalStore();         // PC foi atualizado para a posição final correta.
    }

    // preenche a lista de comandos
    protected void fillCommandsList() {
        commandsList = new ArrayList<String>();
        commandsList.add("add");   		//0
        commandsList.add("sub");   		//1
        commandsList.add("jmp");   		//2
        commandsList.add("jz");    		//3
        commandsList.add("jn");    		//4
        commandsList.add("read");  		//5
        commandsList.add("store"); 		//6
        commandsList.add("ldi");   		//7
        commandsList.add("inc");   		//8
        commandsList.add("moveRegReg"); //9
    }

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



    // os microprogramas ja estao implementados no arquivo de exemplo?
    // parei na linha 192 do exemplo de degas
}






