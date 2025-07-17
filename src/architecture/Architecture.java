package architecture;

import components_degas.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

public class Architecture {
    private static Scanner entrada = new Scanner(System.in);

    private boolean simulation;
    private boolean halt;

    // Components
    private Bus extBus;
    private Bus intBus;

    // Memory
    private Memory memory;
    private Memory statusMemory;
    private int memorySize;

    // Registers
    private Register PC;
    private Register IR;
    private Register REG0;
    private Register REG1;
    private Register REG2;
    private Register REG3;
    private Register StkTOP;
    private Register StkBOT;
    private Register Flags;

    // ALU
    private Ula ula;

    // Demux
    private Demux demux;

    // Lists
    private ArrayList<String> commandsList;
    private ArrayList<Register> registersList;

    // Initialize all architecture components
    private void componentsInstances() {
        // Initialize buses
        this.extBus = new Bus();
        this.intBus = new Bus();

        // Initialize registers
        IR = new Register("IR", null, intBus);
        REG0 = new Register("REG0", null, intBus);
        REG1 = new Register("REG1", null, intBus);
        REG2 = new Register("REG2", null, intBus);
        REG3 = new Register("REG3", null, intBus);
        PC = new Register("PC", null, intBus);
        StkTOP = new Register("stkTop", null, intBus);
        StkBOT = new Register("stkBot", null, intBus);
        Flags = new Register(2, intBus);

        fillRegistersList();

        ula = new Ula(extBus, intBus);
        
        statusMemory = new Memory(2, extBus);
        memorySize = 256;
        memory = new Memory(memorySize, extBus);

        demux = new Demux();

        fillCommandsList();
        setupIMul();
    }

    private void fillRegistersList() {
        registersList = new ArrayList<>();
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

    protected void fillCommandsList() {
        commandsList = new ArrayList<>();
        commandsList.add("addRegReg");      // 0
        commandsList.add("addMemReg");      // 1
        commandsList.add("addRegMem");      // 2
        commandsList.add("addImmReg");      // 3
        commandsList.add("subRegReg");      // 4
        commandsList.add("subMemReg");      // 5
        commandsList.add("subRegMem");      // 6
        commandsList.add("subImmReg");      // 7
        commandsList.add("imulMemReg");     // 8
        commandsList.add("imulRegMem");     // 9
        commandsList.add("imulRegReg");     // 10
        commandsList.add("moveMemReg");     // 11
        commandsList.add("moveRegMem");     // 12
        commandsList.add("moveRegReg");     // 13
        commandsList.add("moveImmReg");     // 14
        commandsList.add("inc");            // 15
        commandsList.add("jmp");            // 16
        commandsList.add("jn");             // 17
        commandsList.add("jz");             // 18
        commandsList.add("jeq");            // 19
        commandsList.add("jneq");           // 20
        commandsList.add("jgt");            // 21
        commandsList.add("jlw");            // 22
        commandsList.add("read");           // 23
        commandsList.add("store");          // 24
        commandsList.add("ldi");            // 25
    }

    public void setupIMul() {
        if (memorySize < 256) {
            System.out.println("AVISO: A memória deve ser de 256 posições para a sub-rotina IMUL funcionar.");
            return;
        }
        
        int tempReg1Addr = 250, oneConstAddr = 251, returnAddr = 252, resultAddr = 253, yAddr = 254, xAddr = 255, tempReg0Addr = 255;
        int reg0_id = 1, reg1_id = 2, pc_id = 5;
        int loopAddr = 94, endLoopAddr = 120, epilogueAddr = 123;

        int[] subroutineMachineCode = {
            // Setup
            12, reg0_id, tempReg0Addr,   // move %REG0 tempReg0
            12, reg1_id, tempReg1Addr,   // move %REG1 tempReg1
            25, 0,                      // ldi 0
            12, reg0_id, resultAddr,     // move %REG0 result
            25, 1,                      // ldi 1
            12, reg0_id, oneConstAddr,   // move %REG0 oneConst
            12, reg0_id, tempReg0Addr,   // move %REG0 zeroConst
            
            // Multiplication loop
            11, yAddr, reg0_id,         // move y %REG0
            5, tempReg0Addr, reg0_id,    // sub zeroConst %REG0 (sets flags)
            18, endLoopAddr,             // jz end_loop
            
            // Loop body: result += x
            11, resultAddr, reg0_id,     // move result %REG0
            1, xAddr, reg0_id,           // add x %REG0
            12, reg0_id, resultAddr,     // move %REG0 result
            
            // Decrement y
            11, yAddr, reg0_id,          // move y %REG0
            11, oneConstAddr, reg1_id,   // move 1 %REG1
            4, reg0_id, reg1_id,         // sub %REG0 %REG1 (REG1 = y-1)
            12, reg1_id, yAddr,          // move %REG1 y
            
            16, loopAddr,                // jmp loop
            
            // End loop
            16, epilogueAddr,            // jmp epilogue

            // Epilogue
            11, resultAddr, reg1_id,     // move result %REG1
            11, tempReg0Addr, reg0_id,   // restore REG0
            11, tempReg1Addr, reg1_id,   // restore REG1
            11, returnAddr, pc_id         // move returnAddr %PC
        };

        // Load subroutine into memory
        int memoryAddress = 74;
        for (int value : subroutineMachineCode) {
            memory.getDataList()[memoryAddress++] = value;
        }
    }

    public Architecture() {
        componentsInstances();
        simulation = false;
    }

    public Architecture(boolean sim) {
        componentsInstances();
        simulation = sim;
    }

    // Getters
    public Register getIR() { return IR; }
    public Register getReg0() { return REG0; }
    public Register getReg1() { return REG1; }
    public Register getReg2() { return REG2; }
    public Register getReg3() { return REG3; }
    public Register getStkTOP() { return StkTOP; }
    public Register getStkBOT() { return StkBOT; }
    public Register getFlags() { return Flags; }
    public Bus getIntBus() { return intBus; }
    public Bus getExtBus() { return extBus; }
    public Ula getUla() { return ula; }
    public Memory getMemory() { return memory; }
    public ArrayList<Register> getRegistersList() { return registersList; }
    public ArrayList<String> getCommandsList() { return commandsList; }
    public int getMemorySize() { return memorySize; }

    private void setStatusFlags(int result) {
        Flags.setBit(0, 0);
        Flags.setBit(1, 0);
        if (result == 0) {
            Flags.setBit(0, 1);
        }
        if (result < 0) {
            Flags.setBit(1, 1);
        }
    }

    private void incrementPC() {
        PC.internalRead();      // intBus <- [PC]
        ula.internalStore(1);   // ula.reg2 <- [PC]
        ula.inc();              // ula.reg2 <- [PC] + 1
        ula.internalRead(1);    // intBus <- [PC] + 1
        PC.internalStore();     // PC <- [PC] + 1
    }

    // Helper methods for register access
    private void registersInternalRead() {
        registersList.get(demux.getValue()).internalRead();
    }
    
    private void registersInternalStore() {
        registersList.get(demux.getValue()).internalStore();
    }
    
    private void registersRead() {
        registersList.get(demux.getValue()).read();
    }
    
    private void registersStore() {
        registersList.get(demux.getValue()).store();
    }

    // Microprogram implementations
    public void addRegReg() {
        incrementPC();
        
        // Get regA ID
        PC.internalRead();
        ula.internalStore(0);
        ula.read(0);
        memory.read();
        demux.setValue(extBus.get());
        registersInternalRead();
        ula.internalStore(0);

        // Get regB ID and content
        incrementPC();
        PC.internalRead();
        ula.internalStore(1);
        ula.read(1);
        memory.read();
        demux.setValue(extBus.get());
        registersInternalRead();
        ula.internalStore(1);

        // Execute addition and store result
        ula.add();
        ula.internalRead(1);
        setStatusFlags(intBus.get());
        registersInternalStore();

        incrementPC();
    }

    public void addMemReg() {
        incrementPC();
        
        // Get memory address and value
        PC.internalRead();
        ula.internalStore(0);
        ula.read(0);
        memory.read();
        memory.read();
        ula.store(0);

        // Get reg ID and content
        incrementPC();
        PC.internalRead();
        ula.internalStore(1);
        ula.read(1);
        memory.read();
        demux.setValue(extBus.get());
        registersInternalRead();
        ula.internalStore(1);

        // Execute addition and store result
        ula.add();
        setStatusFlags(intBus.get());
        ula.internalRead(1);
        registersInternalStore();

        incrementPC();
    }

    public void addRegMem() {
        incrementPC();
        
        // Get reg ID and content
        PC.internalRead();
        ula.internalStore(0);
        ula.read(0);
        memory.read();
        demux.setValue(extBus.get());
        registersInternalRead();
        ula.internalStore(0);

        // Get memory address and value
        incrementPC();
        PC.internalRead();
        ula.internalStore(1);
        ula.read(1);
        memory.read();
        memory.read();
        ula.store(1);

        // Execute addition
        ula.add();
        ula.internalRead(1);
        setStatusFlags(intBus.get());

        // Store result back to memory
        PC.internalRead();
        ula.internalStore(0);
        ula.read(0);
        memory.read();
        memory.store();
        
        ula.read(1);
        memory.store();

        incrementPC();
    }

    public void addImmReg() {
        incrementPC();
        
        // Get immediate value
        PC.internalRead();
        ula.internalStore(0);
        ula.read(0);
        memory.read();
        ula.store(0);

        // Get reg ID
        incrementPC();
        PC.internalRead();
        ula.internalStore(1);
        ula.read(1);
        memory.read();
        demux.setValue(extBus.get());

        registersInternalRead();
        ula.internalStore(1);

        // Execute addition and store result
        ula.add();
        ula.internalRead(1);
        registersInternalStore();

        incrementPC();
    }

    public void subRegReg() {
        incrementPC();
        
        // Get regA ID and content
        PC.internalRead();
        ula.internalStore(0);
        ula.read(0);
        memory.read();
        demux.setValue(extBus.get());
        registersInternalRead();
        ula.internalStore(0);

        // Get regB ID and content
        incrementPC();
        PC.internalRead();
        ula.internalStore(1);
        ula.read(1);
        memory.read();
        demux.setValue(extBus.get());
        registersInternalRead();
        ula.internalStore(1);

        // Execute subtraction and store result
        ula.sub();
        ula.internalRead(1);
        setStatusFlags(intBus.get());
        registersInternalStore();

        incrementPC();
    }

    public void subMemReg() {
        incrementPC();
        
        // Get memory address and value
        PC.internalRead();
        ula.internalStore(0);
        ula.read(0);
        memory.read();
        memory.read();
        ula.store(0);

        // Get reg ID and content
        incrementPC();
        PC.internalRead();
        ula.internalStore(1);
        ula.read(1);
        memory.read();
        demux.setValue(extBus.get());
        registersInternalRead();
        ula.internalStore(1);

        // Execute subtraction and store result
        ula.sub();
        ula.internalRead(1);
        setStatusFlags(intBus.get());
        registersInternalStore();

        incrementPC();
    }

    public void subRegMem() {
        incrementPC();
        
        // Get reg ID and content
        PC.internalRead();
        ula.internalStore(0);
        ula.read(0);
        memory.read();
        demux.setValue(extBus.get());
        registersInternalRead();
        ula.internalStore(0);

        // Get memory address and value
        incrementPC();
        PC.internalRead();
        ula.internalStore(1);
        ula.read(1);
        memory.read();
        memory.read();
        ula.store(1);

        // Execute subtraction
        ula.sub();
        ula.internalRead(1);
        setStatusFlags(intBus.get());

        // Store result back to memory
        PC.internalRead();
        ula.internalStore(0);
        ula.read(0);
        memory.read();
        memory.store();
        
        ula.read(1);
        memory.store();

        incrementPC();
    }

    public void subImmReg() {
        incrementPC();
        
        // Get immediate value
        PC.internalRead();
        ula.internalStore(0);
        ula.read(0);
        memory.read();
        ula.store(0);

        // Get reg ID and content
        incrementPC();
        PC.internalRead();
        ula.internalStore(1);
        ula.read(1);
        memory.read();
        demux.setValue(extBus.get());
        registersInternalRead();
        ula.internalStore(1);

        // Execute subtraction and store result
        ula.sub();
        ula.internalRead(1);
        setStatusFlags(intBus.get());
        registersInternalStore();

        incrementPC();
    }

    private void writeToMemory(int address, int data) {
        intBus.put(data);
        ula.internalStore(0);
        intBus.put(address);
        ula.internalStore(1);

        ula.read(1);
        memory.store();
        ula.read(0);
        memory.store();
    }

    public void imulMemReg() {
        int subroutineAddr = 74, returnAddr = 121, resultAddr = 253, xAddr = 255, yAddr = 254, epilogueAddr = 118;
        int pc_id = 5, reg0_id = 1, reg1_id = 2, tempReg0Addr = 255, tempReg1Addr = 250;

        // Get memory value and store in x
        incrementPC();
        PC.internalRead(); ula.internalStore(0); ula.read(0); memory.read(); memory.read(); ula.store(0);
        ula.internalRead(0);
        writeToMemory(xAddr, intBus.get());

        // Get reg value and store in y
        incrementPC();
        PC.internalRead(); ula.internalStore(0); ula.read(0); memory.read(); demux.setValue(extBus.get());
        registersInternalRead(); ula.internalStore(1);
        int destRegId = demux.getValue();
        ula.internalRead(1);
        writeToMemory(yAddr, intBus.get());
        
        // Prepare epilogue and return
        int epilogueWriteAddr = epilogueAddr;
        writeToMemory(epilogueWriteAddr++, 11); writeToMemory(epilogueWriteAddr++, resultAddr); writeToMemory(epilogueWriteAddr++, destRegId);
        writeToMemory(epilogueWriteAddr++, 11); writeToMemory(epilogueWriteAddr++, tempReg0Addr); writeToMemory(epilogueWriteAddr++, reg0_id);
        writeToMemory(epilogueWriteAddr++, 11); writeToMemory(epilogueWriteAddr++, tempReg1Addr); writeToMemory(epilogueWriteAddr++, reg1_id);
        writeToMemory(epilogueWriteAddr++, 11); writeToMemory(epilogueWriteAddr++, returnAddr); writeToMemory(epilogueWriteAddr++, pc_id);
        
        // Save return address
        incrementPC();
        PC.internalRead();
        writeToMemory(returnAddr, PC.getData());

        // Jump to subroutine
        intBus.put(subroutineAddr);
        PC.internalStore();
    }

    public void imulRegMem() {
        int subroutineAddr = 74, returnAddr = 121, resultAddr = 123, xAddr = 255, yAddr = 254, epilogueAddr = 118;
        int reg0_id = 1, reg1_id = 2, pc_id = 5, tempReg0Addr = 255, tempReg1Addr = 250;

        // Get reg value and store in x
        incrementPC();
        PC.internalRead(); ula.internalStore(0); ula.read(0); memory.read(); demux.setValue(extBus.get());
        registersInternalRead(); ula.internalStore(0);
        ula.internalRead(0);
        writeToMemory(xAddr, intBus.get());

        // Get memory value and store in y
        incrementPC();
        PC.internalRead(); ula.internalStore(0); ula.read(0); memory.read();
        int memDestAddr = extBus.get();
        memory.read(); ula.store(1);
        ula.internalRead(1);
        writeToMemory(yAddr, intBus.get());

        // Prepare epilogue and return
        int epilogueWriteAddr = epilogueAddr;
        writeToMemory(epilogueWriteAddr++, 11); writeToMemory(epilogueWriteAddr++, resultAddr); writeToMemory(epilogueWriteAddr++, reg0_id);
        writeToMemory(epilogueWriteAddr++, 12); writeToMemory(epilogueWriteAddr++, reg0_id); writeToMemory(epilogueWriteAddr++, memDestAddr);
        writeToMemory(epilogueWriteAddr++, 11); writeToMemory(epilogueWriteAddr++, tempReg0Addr); writeToMemory(epilogueWriteAddr++, reg0_id);
        writeToMemory(epilogueWriteAddr++, 11); writeToMemory(epilogueWriteAddr++, tempReg1Addr); writeToMemory(epilogueWriteAddr++, reg1_id);
        writeToMemory(epilogueWriteAddr++, 11); writeToMemory(epilogueWriteAddr++, returnAddr); writeToMemory(epilogueWriteAddr++, pc_id);
        
        // Save return address
        incrementPC();
        PC.internalRead();
        writeToMemory(returnAddr, PC.getData());

        // Jump to subroutine
        intBus.put(subroutineAddr);
        PC.internalStore();
    }

    public void imulRegReg() {
        int subroutineAddr = 74, returnAddr = 121, resultAddr = 123, xAddr = 255, yAddr = 254, epilogueAddr = 118;
        int pc_id = 5, reg0_id = 1, reg1_id = 2, tempReg0Addr = 255, tempReg1Addr = 250;

        // Get regA value and store in x
        incrementPC();
        PC.internalRead(); ula.internalStore(0); ula.read(0); memory.read(); demux.setValue(extBus.get());
        registersInternalRead(); ula.internalStore(0);
        ula.internalRead(0);
        writeToMemory(xAddr, intBus.get());

        // Get regB value and store in y
        incrementPC();
        PC.internalRead(); ula.internalStore(0); ula.read(0); memory.read(); demux.setValue(extBus.get());
        registersInternalRead(); ula.internalStore(1);
        int destRegId = demux.getValue();
        ula.internalRead(1);
        writeToMemory(yAddr, intBus.get());
        
        // Prepare epilogue and return
        int epilogueWriteAddr = epilogueAddr;
        writeToMemory(epilogueWriteAddr++, 11); writeToMemory(epilogueWriteAddr++, resultAddr); writeToMemory(epilogueWriteAddr++, destRegId);
        writeToMemory(epilogueWriteAddr++, 11); writeToMemory(epilogueWriteAddr++, tempReg0Addr); writeToMemory(epilogueWriteAddr++, reg0_id);
        writeToMemory(epilogueWriteAddr++, 11); writeToMemory(epilogueWriteAddr++, tempReg1Addr); writeToMemory(epilogueWriteAddr++, reg1_id);
        writeToMemory(epilogueWriteAddr++, 11); writeToMemory(epilogueWriteAddr++, returnAddr); writeToMemory(epilogueWriteAddr++, pc_id);
        
        // Save return address
        incrementPC();
        PC.internalRead();
        writeToMemory(returnAddr, PC.getData());

        // Jump to subroutine
        intBus.put(subroutineAddr);
        PC.internalStore();
    }

    public void moveMemReg() {
        incrementPC();
        
        // Get pointer address
        PC.internalRead();
        ula.internalStore(1);
        ula.read(1);
        memory.read();
        ula.store(0);

        // Get reg ID
        incrementPC();
        PC.internalRead();
        ula.internalStore(1);
        ula.read(1);
        memory.read();
        demux.setValue(extBus.get());

        // Get final value and store in register
        ula.read(0);
        memory.read();
        ula.store(1);
        ula.internalRead(1);
        registersInternalStore();

        incrementPC();
    }

    public void moveRegMem() {
        incrementPC();
        
        // Get reg content
        PC.internalRead();
        ula.internalStore(0);
        ula.read(0);
        memory.read();
        demux.setValue(extBus.get());
        registersInternalRead();
        ula.internalStore(0);

        // Get memory address
        incrementPC();
        PC.internalRead();
        ula.internalStore(1);
        ula.read(1);
        memory.read();
        ula.store(1);

        // Store to memory
        ula.read(1);
        memory.store();
        ula.read(0);
        memory.store();

        incrementPC();
    }

    public void moveRegReg() {
        incrementPC();
        
        // Get source reg content
        PC.internalRead();
        ula.internalStore(0);
        ula.read(0);
        memory.read();
        demux.setValue(extBus.get());
        registersInternalRead();
        ula.internalStore(0);

        // Get destination reg ID
        incrementPC();
        PC.internalRead();
        ula.internalStore(1);
        ula.read(1);
        memory.read();
        demux.setValue(extBus.get());

        // Store to destination reg
        ula.internalRead(0);
        registersInternalStore();

        incrementPC();
    }

    public void moveImmReg() {
        incrementPC();
        
        // Get immediate value
        PC.internalRead();
        ula.internalStore(0);
        ula.read(0);
        memory.read();
        ula.store(0);

        // Get reg ID
        incrementPC();
        PC.internalRead();
        ula.internalStore(1);
        ula.read(1);
        memory.read();
        demux.setValue(extBus.get());

        // Store to register
        ula.internalRead(0);
        registersInternalStore();

        incrementPC();
    }

    public void incReg() {
        incrementPC();
        
        // Get reg ID and content
        PC.internalRead(); ula.internalStore(0); ula.read(0); memory.read();
        demux.setValue(extBus.get());
        registersInternalRead();
        ula.internalStore(1);

        // Increment and store back
        ula.inc();
        ula.internalRead(1);
        registersInternalStore();

        incrementPC();
    }
    
    public void jmp() {
        incrementPC();
        
        // Get jump address
        PC.internalRead();
        ula.internalStore(0);
        ula.read(0);
        memory.read();
        ula.store(0);
        ula.internalRead(0);
        PC.internalStore();
    }

    public void jn() {
        // Calculate continuation address (PC+2)
        PC.internalRead();
        ula.internalStore(1);
        ula.inc();
        ula.inc();
        ula.read(1);
        statusMemory.storeIn0();

        // Get jump address from [PC+1]
        incrementPC();
        PC.internalRead();
        ula.internalStore(1);
        ula.read(1);
        memory.read();
        statusMemory.storeIn1();

        // Use negative flag to select address
        extBus.put(Flags.getBit(1));
        statusMemory.read();
        
        ula.store(0);
        ula.internalRead(0);
        PC.internalStore();
    }

    public void jz() {
        // Calculate continuation address (PC+2)
        PC.internalRead();
        ula.internalStore(1);
        ula.inc();
        ula.inc();
        ula.read(1);
        statusMemory.storeIn0();

        // Get jump address from [PC+1]
        incrementPC();
        PC.internalRead();
        ula.internalStore(1);
        ula.read(1);
        memory.read();
        statusMemory.storeIn1();

        // Use zero flag to select address
        extBus.put(Flags.getBit(0));
        statusMemory.read();
        
        ula.store(0);
        ula.internalRead(0);
        PC.internalStore();
    }

    public void jeq() {
        // Get regA and regB values
        incrementPC();
        PC.internalRead(); ula.internalStore(0); ula.read(0); memory.read(); demux.setValue(extBus.get());
        registersInternalRead(); ula.internalStore(0);

        incrementPC();
        PC.internalRead(); ula.internalStore(1); ula.read(1); memory.read(); demux.setValue(extBus.get());
        registersInternalRead(); ula.internalStore(1);

        // Compare and set flags
        ula.sub();
        setStatusFlags(intBus.get());

        // Prepare addresses
        PC.internalRead();
        ula.internalStore(1);
        ula.inc();
        ula.inc();
        ula.read(1);
        statusMemory.storeIn0();

        incrementPC();
        PC.internalRead();
        ula.internalStore(1);
        ula.read(1);
        memory.read();
        statusMemory.storeIn1();

        // Use zero flag to select address
        extBus.put(Flags.getBit(0));
        statusMemory.read();
        
        ula.store(0);
        ula.internalRead(0);
        PC.internalStore();
    }

    public void jneq() {
        // Get regA and regB values
        incrementPC();
        PC.internalRead(); ula.internalStore(0); ula.read(0); memory.read(); demux.setValue(extBus.get());
        registersInternalRead(); ula.internalStore(0);

        incrementPC();
        PC.internalRead(); ula.internalStore(1); ula.read(1); memory.read(); demux.setValue(extBus.get());
        registersInternalRead(); ula.internalStore(1);

        // Compare and set flags
        ula.sub();
        setStatusFlags(intBus.get());

        // Prepare addresses (inverted logic)
        incrementPC();
        PC.internalRead();
        ula.internalStore(0);
        ula.read(0);
        memory.read();
        statusMemory.storeIn0();

        PC.internalRead();
        ula.internalStore(1);
        ula.inc();
        ula.read(1);
        statusMemory.storeIn1();

        // Use zero flag to select address
        extBus.put(Flags.getBit(0));
        statusMemory.read();
        
        ula.store(0);
        ula.internalRead(0);
        PC.internalStore();
    }

    public void jgt() {
        // Get regA and regB values
        incrementPC();
        PC.internalRead(); ula.internalStore(0); ula.read(0); memory.read(); demux.setValue(extBus.get());
        registersInternalRead(); ula.internalStore(0);

        incrementPC();
        PC.internalRead(); ula.internalStore(1); ula.read(1); memory.read(); demux.setValue(extBus.get());
        registersInternalRead(); ula.internalStore(1);

        // Compare and set flags
        ula.sub();
        setStatusFlags(intBus.get());

        // Prepare addresses
        PC.internalRead();
        ula.internalStore(1);
        ula.inc();
        ula.inc();
        ula.read(1);
        statusMemory.storeIn0();

        incrementPC();
        PC.internalRead();
        ula.internalStore(1);
        ula.read(1);
        memory.read();
        statusMemory.storeIn1();

        // Use combination of flags to select address
        extBus.put(Flags.getBit(1));
        ula.store(0);

        extBus.put(Flags.getBit(0));
        ula.store(1);

        ula.add();
        setStatusFlags(intBus.get());

        extBus.put(Flags.getBit(0));
        statusMemory.read();
        
        ula.store(0);
        ula.internalRead(0);
        PC.internalStore();
    }

    public void jlw() {
        // Get regA and regB values
        incrementPC();
        PC.internalRead(); ula.internalStore(0); ula.read(0); memory.read(); demux.setValue(extBus.get());
        registersInternalRead(); ula.internalStore(0);

        incrementPC();
        PC.internalRead(); ula.internalStore(1); ula.read(1); memory.read(); demux.setValue(extBus.get());
        registersInternalRead(); ula.internalStore(1);

        // Compare and set flags
        ula.sub();
        setStatusFlags(intBus.get());

        // Prepare addresses
        PC.internalRead();
        ula.internalStore(1);
        ula.inc();
        ula.inc();
        ula.read(1);
        statusMemory.storeIn0();

        incrementPC();
        PC.internalRead();
        ula.internalStore(1);
        ula.read(1);
        memory.read();
        statusMemory.storeIn1();

        // Use negative flag to select address
        extBus.put(Flags.getBit(1));
        statusMemory.read();
        
        ula.store(0);
        ula.internalRead(0);
        PC.internalStore();
    }

    public void read() {
        incrementPC();
        
        // Get pointer address
        PC.internalRead();
        ula.internalStore(1);
        ula.read(1);
        memory.read();
        ula.store(0);

        // Get final value and store in REG0
        ula.read(0);
        memory.read();
        ula.store(1);
        ula.internalRead(1);
        demux.setValue(1); // REG0
        registersInternalStore();

        incrementPC();
    }

    public void store() {
        incrementPC();
        
        // Get memory address
        PC.internalRead();
        ula.internalStore(1);
        ula.read(1);
        memory.read();
        ula.store(1);

        // Get REG0 value
        demux.setValue(1); // REG0
        registersInternalRead();
        ula.internalStore(0);

        // Store to memory
        ula.read(1);
        memory.store();
        ula.read(0);
        memory.store();

        incrementPC();
    }

    public void ldi() {
        // Get immediate address (PC+1)
        PC.internalRead();
        ula.internalStore(1);
        ula.inc();
        ula.read(1);

        // Get immediate value
        memory.read();
        ula.store(1);
        ula.internalRead(1);
        
        // Store to REG0
        demux.setValue(1); // REG0
        registersInternalStore();

        // Update PC to PC+2
        PC.internalRead();
        ula.internalStore(1);
        ula.inc();
        ula.inc();
        ula.internalRead(1);
        PC.internalStore();
    }

    public void readExec(String filename) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(filename+".dxf"));
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
    
    public void controlUnitEexec() {
        halt = false;
        while (!halt) {
            fetch();
            decodeExecute();
        }
    }
    
    private void fetch() {
        PC.internalRead();
        ula.internalStore(0);
        ula.read(0);
        memory.read();
        ula.store(0);
        ula.internalRead(0);
        IR.internalStore();
        simulationFetch();
    }

    private void decodeExecute() {
        IR.internalRead();
        int command = intBus.get();
        simulationDecodeExecuteBefore(command);
        
        switch (command) {
            case 0: addRegReg(); break;
            case 1: addMemReg(); break;
            case 2: addRegMem(); break;
            case 3: addImmReg(); break;
            case 4: subRegReg(); break;
            case 5: subMemReg(); break;
            case 6: subRegMem(); break;
            case 7: subImmReg(); break;
            case 8: imulMemReg(); break;
            case 9: imulRegMem(); break;
            case 10: imulRegReg(); break;
            case 11: moveMemReg(); break;
            case 12: moveRegMem(); break;
            case 13: moveRegReg(); break;
            case 14: moveImmReg(); break;
            case 15: incReg(); break;
            case 16: jmp(); break;
            case 17: jn(); break;
            case 18: jz(); break;
            case 19: jeq(); break;
            case 20: jneq(); break;
            case 21: jgt(); break;
            case 22: jlw(); break;
            case 23: read(); break;
            case 24: store(); break;
            case 25: ldi(); break;
            default: halt = true; break;
        }
        
        if (simulation) { simulationDecodeExecuteAfter(); }
    }

    // Simulation methods
    private void simulationDecodeExecuteBefore(int command) {
        if(simulation){
            System.out.println("----------BEFORE Decode and Execute phases--------------");
            int parameter = 0;
            
            for (Register r : registersList) {
                System.out.println(r.getRegisterName() + ": " + r.getData());
            }

            String instruction;
            if (command != -1) {
                instruction = commandsList.get(command);
            } else {
                instruction = "END";
            }

            if (hasOperands(instruction)) {
                parameter = memory.getDataList()[PC.getData() + 1];
                System.out.println("Instruction: " + instruction + " " + parameter);
            } else {
                System.out.println("Instruction: " + instruction);
            }

            if ("read".equals(instruction)) {
                System.out.println("memory[" + parameter + "]=" + memory.getDataList()[parameter]);
            }
        }

        System.out.print("Memória: ");
        for (int i = 0; i < 10; i++) {
            System.out.printf("%d ", memory.getDataList()[memorySize - 10 + i]);
        }
        System.out.println();
    }

    private void simulationDecodeExecuteAfter() {
        if(simulation){
            System.out.println("-----------AFTER Decode and Execute phases--------------");
            System.out.println("Internal Bus: " + intBus.get());
            System.out.println("External Bus: " + extBus.get());
            
            for (Register r : registersList) {
                System.out.println(r.getRegisterName() + ": " + r.getData());
            }

            System.out.println("Press <Enter>");
            entrada.nextLine();
        }
    }

    private void simulationFetch() {
        if (simulation) {
            System.out.println("-------Fetch Phase------");
            System.out.println("PC: " + PC.getData());
            System.out.println("IR: " + IR.getData());
        }
    }

    private boolean hasOperands(String instruction) {
        return !"inc".equals(instruction);
    }

    public static void main(String[] args) throws IOException {
        Architecture arch = new Architecture(true);
        arch.readExec("simuladorArch/testes/move");
        arch.controlUnitEexec();
        entrada.close();
    }
}


