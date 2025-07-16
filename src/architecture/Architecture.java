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

    // move %regA <mem>
    public void moveRegMem() {
        // coloca o endereco do pc no bus externo
        PC.read();  
        
        // memoria le o endereco e coloca o id no bus
        memory.read();
        int regId = extBus.get();
        
        // incrementa o pc p/ apontar p/ o prox. operando
        PC.read();
        ula.store(0);
        extBus.setData(1);
        ula.store(1);
        ula.add();
        ula.read(2);
        PC.store();

        // coloca o endereco do PC no bus
        PC.read();

        // memoria le o endereco e coloca o endereco de destino no bus
        memory.read(); 
        int memAddress = extBus.get();

        // incrementa o pc novamente p/ apontar p/ proxima instrucao
        PC.read();
        ula.store(0);
        extBus.setData(1);
        ula.store(1);
        ula.add();
        ula.read(2);
        PC.store();

        // configurando o demux p/ selecionar o registrador de origem
        demux.setValue(regId);

        // le o valor do reg escolhido e poe no bus externo
        registersList.get(demux.getValue()).read();
        
        // guarda o valor numa variavel local p/ o bus ser usado novamente
        int valueStored = extBus.getData();

        // enviar o endereco p/ a memoria
        extBus.setData(memAddress);
        memory.store();

        // enviar o dado que sera escrito no endereco
        extBus.setData(valueStored);
        memory.store();
    }

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






