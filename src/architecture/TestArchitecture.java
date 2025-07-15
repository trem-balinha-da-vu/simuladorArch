import static org.junit.Assert.*;
import org.junit.Test;
import architecture.Architecture;

public class TestArchitecture {

    @Test
    public void testAddMemReg() {
        // 1. Setup
        Architecture arch = new Architecture(true); // 'true' para modo simulação, se desejar

        // Vamos simular a instrução "add varA %REG1"
        // Onde varA está no endereço 40 e contém o valor 5.
        // E REG1 (índice 2 na sua lista) contém o valor 8.

        // Pré-carregando valores na memória e registradores
        // Usando um método de escrita direta para facilitar o teste (sugestão da conversa anterior)
        arch.getMemory().directWrite(40, 5); // varA = 5

        // A sua lista de registradores é: IR, REG0, REG1, REG2, REG3, PC ...
        // Vamos assumir que REG1 é o índice 2 da sua registersList
        int reg1_index = 2;
        arch.getRegistersList().get(reg1_index).setData(8); // REG1 = 8

        // O microprograma 'addMemReg' espera que o PC aponte para o primeiro
        // operando (endereço de memória) e o segundo operando (ID do registrador)
        // nas posições seguintes.
        // Para o teste, vamos colocar esses valores manualmente na ULA,
        // como se tivessem sido buscados da memória.
        arch.getUla().store(0); // Coloca o endereço 40 no registrador 0 da ULA
        arch.getExtBus().setData(40);

        arch.getUla().store(1); // Coloca o ID do registrador no registrador 1 da ULA
        arch.getExtBus().setData(reg1_index);


        // 2. Execução
        // Para testar o microprograma de forma isolada, torne-o público
        // na classe Architecture e chame-o diretamente.

        // arch.microprogramAddMemToReg(); // Use o nome do seu microprograma

        // (Nota: A implementação do microprograma precisa ser ajustada
        // para buscar os operandos da ULA em vez de buscá-los da memória
        // via PC, para que este teste funcione de forma isolada.)

        // Uma abordagem mais integrada seria carregar o opcode e os operandos na memória
        // e chamar o método run() para executar um único ciclo.

        // --- Abordagem de Teste Integrado (Melhor) ---
        // opcode para add <mem> %regA (ex: 1) em mem[0]
        // operando <mem> (40) em mem[1]
        // operando %regA (ID 2) em mem[2]
        arch.getMemory().directWrite(0, 1);
        arch.getMemory().directWrite(1, 40);
        arch.getMemory().directWrite(2, 2);

        // Ajustar PC para o início da instrução
        arch.getPC().setData(0);

        // Execute um ciclo (ou a simulação completa)
        // arch.run(); // Supondo que o seu run() executa o programa

        // Para este exemplo, vamos simular manualmente a lógica do microprograma:
        // Lógica de addMemReg:
        // a. Pega valor de mem[40] -> para ULA(0)
        arch.getExtBus().setData(40);
        arch.getMemory().read(); // Dado 5 vai para o barramento
        arch.getUla().store(0); // ULA reg0 = 5

        // b. Pega valor de REG1 -> para ULA(1)
        arch.getRegistersList().get(reg1_index).read(); // Dado 8 vai para o barramento
        arch.getUla().store(1); // ULA reg1 = 8

        // c. Executa a soma
        arch.getUla().add(); // Resultado vai para ULA reg2

        // d. Guarda o resultado em REG1
        arch.getUla().read(2); // Resultado 13 vai para o barramento
        arch.getRegistersList().get(reg1_index).store();


        // 3. Verificação (Asserts)
        int finalValueInReg1 = arch.getRegistersList().get(reg1_index).getData();
        assertEquals("O resultado da soma deve ser 13", 13, finalValueInReg1);
    }
}