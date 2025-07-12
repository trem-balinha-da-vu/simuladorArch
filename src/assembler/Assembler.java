package assembler;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import components.Register;
import architecture.Architecture;

public class Assembler {

    private ArrayList<String> lines;
    private ArrayList<String> objProgram;
    private ArrayList<String> execProgram;
    private Architecture arch;
    private ArrayList<String>commands;
    //tentando mudar um pouco a lógica: em vez de duas listas, um map para os labels com a chave sendo String label e o valor Integer endereço 
    private Map<String, Integer> labelsMap;

    private ArrayList<String>variables;

    public Assembler() {
        lines = new ArrayList<>();
        labelsMap = new HashMap<>();
        variables = new ArrayList<>();
        objProgram = new ArrayList<>();
        execProgram = new ArrayList<>();
        arch = new Architecture();
        commands = arch.getCommandsList();
    }

    // getters
    public ArrayList<String> getObjProgram() { return objProgram; }
    protected ArrayList<String> getVariables() { return variables; }
    protected ArrayList<String> getExecProgram() { return execProgram; }
    protected void setLines(ArrayList<String> lines) { this.lines = lines; }
    protected void setExecProgram(ArrayList<String> lines) { this.execProgram = lines; }

    /*
     * An assembly program is always in the following template
     * <variables>
     * <commands>
     * Obs.
     * 		variables names are always started with alphabetical char
     * 	 	variables names must contains only alphabetical and numerical chars
     *      variables names never uses any command name
     * 		names ended with ":" identifies labels i.e. address in the memory
     * 		Commands are only that ones known in the architecture. No comments allowed
     *
     * 		The assembly file must have the extention .dsf
     * 		The executable file must have the extention .dxf
     */
    // ler um arquivo inteiro em assembly
    public void read(String filename) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(filename+".dsf"));
        String linha;
        while ((linha = br.readLine()) != null) {
            lines.add(linha);
        }
        br.close();
    }

    // analisa as strings em linhas, gerando um codigo de maquina para cada
    public void parse() {
        for (String s : this.lines) {
            String[] tokens = s.split(" ");
            int commandNumber = findCommandNumber(tokens); //só vai retornar -1 se não for um comando. Do contrário, retorna a posição de memória do comando na lista vinda de arch
            if (commandNumber >= 0) {
                proccessCommand(tokens, commandNumber);
            } else if (tokens[0].endsWith(":")) {//é um label
                String label = tokens[0].substring(0, tokens[0].length() - 1);
                labelsMap.put(label, objProgram.size());
            } else {//é uma variavel
                this.variables.add(tokens[0]);
            }
        }
    }


    // processa um comando, colocando ele e os parametros
    // (se existirem), no final da array
    protected void proccessCommand(String[] tokens, int commandNumber){
        String parameter ="";
        String parameter2 = "";
        String parameter3 = "";
        if (commandNumber == 0) { //must to proccess an addRegReg command
			parameter = tokens[1];
			parameter2 = tokens[2];
		}
		if (commandNumber == 1) { //must to proccess an addMemReg command
			parameter = tokens[1];
			parameter2 = tokens[2];
			parameter = "&"+parameter;
		}
		if (commandNumber == 2) { //must to proccess an addRegMem command
			parameter = tokens[1];
			parameter2 = tokens[2];
			parameter2 = "&"+parameter2;
		}
		if (commandNumber == 3) { //must to proccess an addImmReg command
			parameter = tokens[1];
			parameter2 = tokens[2];
		}
		if (commandNumber == 4) { //must to proccess an subRegReg command
			parameter = tokens[1];
			parameter2 = tokens[2];
		}
		if (commandNumber == 5) { //must to proccess an subMemReg command
			parameter = tokens[1];
			parameter2 = tokens[2];
			parameter = "&"+parameter;
		}
		if (commandNumber == 6) { //must to proccess an subRegMem command
			parameter = tokens[1];
			parameter2 = tokens[2];
			parameter2 = "&"+parameter2;
		}
		if (commandNumber == 7) { //must to proccess an subImmReg command
			parameter = tokens[1];
			parameter2 = tokens[2];
		}
		if (commandNumber == 8) { //must to proccess an imulMemReg command
			parameter = tokens[1];
			parameter2 = tokens[2];
			parameter = "&"+parameter;
		}
		if (commandNumber == 9) { //must to proccess an imulRegMem command
			parameter = tokens[1];
			parameter2 = tokens[2];
			parameter2 = "&"+parameter2;
		}
		if (commandNumber == 10) { //must to proccess an imulRegReg command
			parameter = tokens[1];
			parameter2 = tokens[2];
		}
		if (commandNumber == 11) { //must to proccess an moveMemReg command
			parameter = tokens[1];
			parameter2 = tokens[2];
			parameter = "&"+parameter;
		}
		if (commandNumber == 12) { //must to proccess an moveRegMem command
			parameter = tokens[1];
			parameter2 = tokens[2];
			parameter2 = "&"+parameter2;
		}
		if (commandNumber == 13) { //must to proccess an moveRegReg command
			parameter = tokens[1];
			parameter2 = tokens[2];
		}
		if (commandNumber == 14) { //must to proccess an moveImmReg command
			parameter = tokens[1];
			parameter2 = tokens[2];
		}
		if (commandNumber == 15) { //must to proccess an incReg command
			parameter = tokens[1];
		}
		if (commandNumber == 16) { //must to proccess an jmp command
			parameter = tokens[1];
			parameter = "&"+parameter; 
		}
		if (commandNumber == 17) { //must to proccess an jn command
			parameter = tokens[1];
			parameter = "&"+parameter;
		}
		if (commandNumber == 18) { //must to proccess an jz command
			parameter = tokens[1];
			parameter = "&"+parameter;
		}
		if (commandNumber == 19) { //must to proccess an jeq command
			parameter = tokens[1];
			parameter2 = tokens[2];
			parameter3 = tokens[3];
			parameter3 = "&"+parameter3;
		}
		if (commandNumber == 20) { //must to proccess an jneq command
			parameter = tokens[1];
			parameter2 = tokens[2];
			parameter3 = tokens[3];
			parameter3 = "&"+parameter3; 
		}
		if (commandNumber == 21) { //must to proccess an jgt command
			parameter = tokens[1];
			parameter2 = tokens[2];
			parameter3 = tokens[3];
			parameter3 = "&"+parameter3; 
		}
		if (commandNumber == 22) { //must to proccess an jlw command
			parameter = tokens[1];
			parameter2 = tokens[2];
			parameter3 = tokens[3];
			parameter3 = "&"+parameter3; 
		}
		if (commandNumber == 23) { //must to proccess an read command
			parameter = tokens[1];
			parameter = "&"+parameter; 
		}
		if (commandNumber == 24) { //must to proccess an store command
			parameter = tokens[1];
			parameter = "&"+parameter; 
		}
		if (commandNumber == 25) { //must to proccess an ldi command
			parameter = tokens[1];
		}
		objProgram.add(Integer.toString(commandNumber));
		if (!parameter.isEmpty()) {
			objProgram.add(parameter);
		}
		if (!parameter2.isEmpty()) {
			objProgram.add(parameter2);
		}
		if (!parameter3.isEmpty()){
			objProgram.add(parameter3);
		}
    }

    // o metodo a seguir usa os tokens para procurar um comando
    // na lista de comandos e retornar o id dele.
    private int findCommandNumber(String[] tokens) {
		int p = commands.indexOf(tokens[0]);
		if (p<0){ //Se não está na lista, então tem multiplos formatos
			if ("move".equals(tokens[0])) //é um move
				p = proccessMove(tokens);
			else if ("add".equals(tokens[0]))
				p = proccessAdd(tokens); // é um add
			else if ("sub".equals(tokens[0]))
				p = proccessSub(tokens); //é um sub
			else if ("imul".equals(tokens[0]))
				p = proccessImul(tokens); //é um imul
		}
		return p;
	}

    // Processa o comando move 
    private int proccessMove(String[] tokens) {
		String p1 = tokens[1];
		String p2 = tokens[2];
		int p=-1;
		if ((p1.startsWith("%"))&&(p2.startsWith("%"))) { //moveRegReg
			p = commands.indexOf("moveRegReg");
		}
		else if ((p1.startsWith("%"))&&(p2.matches("^[A-Za-z].*"))) { //moveRegMem
			p = commands.indexOf("moveRegMem");
		}
		else if ((p1.matches("^[A-Za-z].*"))&&(p2.startsWith("%"))) { //moveMemReg
			p = commands.indexOf("moveMemReg");
		}
		else if ((p1.matches("[-]*[0-9]+"))&&(p2.startsWith("%"))) { //moveImmReg
			p = commands.indexOf("moveImmReg");
		}
		return p;
	}
	
    // Processa o comando add
	private int proccessAdd(String[] tokens) {
		String p1 = tokens[1];
		String p2 = tokens[2];
		int p=-1;
		if ((p1.startsWith("%"))&&(p2.startsWith("%"))) { // addRegReg
			p = commands.indexOf("addRegReg");
		}
		else if ((p1.startsWith("%"))&&(p2.matches("^[A-Za-z].*"))) { //addRegMem
			p = commands.indexOf("addRegMem");
		}
		else if ((p1.matches("^[A-Za-z].*"))&&(p2.startsWith("%"))) { //addMemReg
			p = commands.indexOf("addMemReg");
		}
		else if ((p1.matches("[-]*[0-9]+"))&&(p2.startsWith("%"))) { //moveRegReg
			p = commands.indexOf("addImmReg");
		}
		return p;
	}

    // Processa o comando sub
	private int proccessSub(String[] tokens) {
		String p1 = tokens[1];
		String p2 = tokens[2];
		int p=-1;
		if ((p1.startsWith("%"))&&(p2.startsWith("%"))) { //subRegReg
			p = commands.indexOf("subRegReg");
		}
		else if ((p1.matches("^[A-Za-z].*"))&&(p2.startsWith("%"))) { //subMemReg
			p = commands.indexOf("subMemReg");
		}
		else if ((p1.startsWith("%"))&&(p2.matches("^[A-Za-z].*"))) { //subRegMem
			p = commands.indexOf("subRegMem");
		}
		else if ((p1.matches("[-]*[0-9]+"))&&(p2.startsWith("%"))) { //subImmReg
			p = commands.indexOf("subImmReg");
		}
		return p;
	}
    // Processa o comando imul
	private int proccessImul(String[] tokens) {
		String p1 = tokens[1];
		String p2 = tokens[2];
		int p=-1;
		if ((p1.matches("^[A-Za-z].*"))&&(p2.startsWith("%"))) { //imulMemReg
			p = commands.indexOf("imulMemReg");
		}
		else if ((p1.startsWith("%"))&&(p2.matches("^[A-Za-z].*"))) { //imulRegMem
			p = commands.indexOf("imulRegMem");
		}
		else if ((p1.startsWith("%"))&&(p2.startsWith("%"))) { //imulRegReg
			p = commands.indexOf("imulRegReg");
		}
		else if ((p1.matches("[-]*[0-9]+"))&&(p2.startsWith("%"))) { //subImmReg
			p = commands.indexOf("imulImmReg");
		}
		return p;
	}





    // esse metodo cria um programa executavel do objProgram
    // passo 1: conferir se todas as variaveis e labels mencionadas
    // no objeto estao declaradas no source
    // passo 2: alocar endereços de memoria (espaços), do fim ate
    // o começo (stack) para guardar mais variaveis
    // passo 3: identificar posicoes de memoria para o labels
    // passo 4: fazer o executavel ao substituir o labels e as
    // variaveis pelos endereços de memoria correspondentes
    public void makeExecutable(String filename) throws IOException {
        if (!checkLabelsAndVariables())
            return;
        execProgram = new ArrayList<>(objProgram);  
        replaceAllVariables();
        replaceLabels(); // substituindo todos os labels pelos endereços que eles se referem
        replaceRegisters(); // substituindo todos os registradores pelo id de registrador que eles se referem
        saveExecFile(filename);
        System.out.println("Finished");
    }

    // esse metodo confere se todas as labels e as variaveis no
    // objProgram estavam no source.
    // As coleçoes de labels e das variaveis sao usadas para isso.
    protected boolean checkLabelsAndVariables() {
        System.out.println("Checking labels and variables");
        for (String line : execProgram) {
            if (line.startsWith("&")) {
                String name = line.substring(1);
                boolean found = labelsMap.containsKey(name) || variables.contains(name);
                if (!found) {
                    System.out.println("FATAL ERROR! Variable or label " + name + " not declared!");
                    return false;
                }
            }
        }
        return true;
    }

    
    // esse metodo substitui todas as variaveis pelos endereços.
    // os endereços das variaveis começam no fim da memoria e
    // diminuem, criando uma pilha
    protected void replaceAllVariables() {
        int position = arch.getMemorySize()-1; //starting from the end of the memory
        for (String var : this.variables) { //scanning all variables
            replaceVariable(var, position);
            position --;
        }
    }

    // esse metodo substitui todas as ocorrencias de nomes de
    // variaveis encontradas no objProgram pelo seu endereço
    // no programa executavel
    protected void replaceVariable(String var, int position) {
        var = "&"+var;
        int i=0;
        for (String s:execProgram) {
            if (s.equals(var)) {
                s = Integer.toString(position);
                execProgram.set(i, s);
            }
            i++;
        }
    }

    // esse metodo substitui todas as labels no programa executavel
    // pelos endereços correspondentes as quais eles referem.
    /**
     * Método novo (com Map):
     *    Varre direto cada linha de execProgram.
     *    Se encontrar uma linha que começa com & (ou seja, representa um label), pega o label, busca no labelsMap e substitui por seu endereço.
     * Agora é um loop só, mais rápido e direto. O efeito é o mesmo: todas as linhas com &label serão trocadas pelo valor inteiro do endereço desse label
     */
    protected void replaceLabels() {
        for (int i = 0; i < execProgram.size(); i++) {
            String line = execProgram.get(i);
            if (line.startsWith("&")) {
                String name = line.substring(1);
                if (labelsMap.containsKey(name)) {
                    execProgram.set(i, Integer.toString(labelsMap.get(name)));
                }
            }
        }
    }


    // esse metodo substitui todos os nomes dos registradores
    // pelo id correspondente.
    // os nomes dos registradores devem ser prefixados com %
    protected void replaceRegisters() {
        int p=0;
        for (String line:execProgram) {
            if (line.startsWith("%")){ // essa linha é um registrador
                line = line.substring(1, line.length());
                int regId = searchRegisterId(line, arch.getRegistersList());
                String newLine = Integer.toString(regId);
                execProgram.set(p, newLine);
            }
            p++;
        }
    }

    // esse metodo salva a coleçao do arquivo executavel no
    // arquivo de saida.
    private void saveExecFile(String filename) throws IOException {
        File file = new File(filename+".dxf");
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        for (String l : execProgram)
            writer.write(l+"\n");
        writer.write("-1"); //-1 é uma flag indicando que o programa terminou
        writer.close();
    }

    // esse metodo busca por um registrador na lista de
    // registradores da arquitetura pelo nome.
    private int searchRegisterId(String line, ArrayList<Register> registersList) {
        int i=0;
        for (Register r:registersList) {
            if (line.equals(r.getRegisterName())) {
                return i;
            }
            i++;
        }
        return -1;
    }

    public static void main(String[] args) throws IOException {
        String filename = args[0];
        Assembler assembler = new Assembler();
        System.out.println("Reading source assembler file: "+filename+".dsf");
        assembler.read(filename);
        System.out.println("Generating the object program");
        assembler.parse();
        System.out.println("Generating executable: "+filename+".dxf");
        assembler.makeExecutable(filename);
    }
}
