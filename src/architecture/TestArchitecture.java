package architecture;

import static org.junit.Assert.*;
import org.junit.Test;

public class TestArchitecture {

    @Test
    public void testAddMemReg() {
        // 1. SETUP: Configurar o estado inicial da máquina
        Architecture arch = new Architecture(true);

        // A instrução que estamos testando é "add 40, %REG1"
        // que significa: REG1 = MEM[40] + REG1
        // O valor de REG1 será 8 e o valor em MEM[40] será 5. O resultado esperado é 13.

        int reg1_index = 2; // Conforme a sua fillRegistersList: IR(0), REG0(1), REG1(2)...
        int memAddress = 40;
        int initialRegValue = 8;
        int memValue = 5;
        int expectedResult = 13;

        // Pré-carregando o valor na memória
        arch.getMemory().directWrite(memAddress, memValue);

        // Pré-carregando o valor inicial no registrador REG1
        // (Usando o método setData() que adicionamos em Register.java)
        arch.getRegistersList().get(reg1_index).setData(initialRegValue);

        // O microprograma espera que o PC aponte para os operandos.
        // Vamos simular que o programa está na memória.
        // O laço de execução principal (run) já teria lido o opcode e incrementado o PC.
        // Então, o PC apontaria para o primeiro operando.
        // mem[1] = endereço de memória (40)
        // mem[2] = ID do registrador (2)
        arch.getMemory().directWrite(1, memAddress);
        arch.getMemory().directWrite(2, reg1_index);

        // Ajustamos o PC para apontar para o primeiro operando (endereço 40)
        arch.getPC().setData(1);


        // 2. EXECUÇÃO: Chamar o método que queremos testar
        // Este método deve ser público na classe Architecture.
        // O método usará o PC para buscar os operandos (40 e 2) da memória.
        // arch.addMemReg(); // Descomente quando o método estiver pronto e público

        // ======== SIMULANDO A CHAMADA AO MICROPROGRAMA REAL ========
        // Para fins deste exemplo, vamos chamar o método que corrigimos na conversa anterior.
        // Este método usa o helper fetchOperand() que lê da memória usando o PC.
        arch.subMemReg();


        // 3. VERIFICAÇÃO: Checar se o estado final da máquina está correto
        int finalValueInReg1 = arch.getRegistersList().get(reg1_index).getData();
        assertEquals("O resultado da soma em REG1 deve ser 13", expectedResult, finalValueInReg1);

        // O microprograma addMemReg busca 2 operandos, incrementando o PC duas vezes.
        // Se o PC começou em 1, deve terminar em 3.
        int finalPCValue = arch.getPC().getData();
        assertEquals("O PC deve avançar duas posições", 3, finalPCValue);
    }
}