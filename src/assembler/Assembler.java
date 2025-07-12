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

import org.hamcrest.core.IsNull;

import components.Register;

import architecture.Architecture;

public class Assembler {
    private ArrayList<String> lines;
    private ArrayList<String> objProgram;
    private ArrayList<String> execProgram;
    private Architecture arch;
    private ArrayList<String>commands;
    private ArrayList<String>labels;
    private ArrayList<Integer> labelsAdresses;
    private ArrayList<String>variables;

    public Assembler() {
        lines = new ArrayList<>();
        labels = new ArrayList<>();
        labelsAdresses = new ArrayList<>();
        variables = new ArrayList<>();
        objProgram = new ArrayList<>();
        execProgram = new ArrayList<>();
        arch = new Architecture();
        commands = arch.getCommandsList();
    }

    // getters
    public ArrayList<String> getObjProgram() { return objProgram; }

    /**
     * These methods getters and set below are used only for TDD purposes
     * @param lines
     */
    protected ArrayList<String> getLabels() {
        return labels;
    }

    protected ArrayList<Integer> getLabelsAddresses() {
        return labelsAdresses;
    }

    protected ArrayList<String> getVariables() {
        return variables;
    }

    protected ArrayList<String> getExecProgram() {
        return execProgram;
    }

    protected void setLines(ArrayList<String> lines) {
        this.lines = lines;
    }

    protected void setExecProgram(ArrayList<String> lines) {
        this.execProgram = lines;
    }

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
        for (String s : lines) {
            String tokens[] = s.split(" ");
            if (findCommandNumber(tokens) >= 0) {    // a linha é um comando
                processCommand(tokens);
            } else {
                // se a linha nao for um comando, entao pode ser uma variavel ou um label
                if (tokens[0].endsWith(":")) {
                    // se termina com : é um label
                    String label = tokens[0].substring(0, tokens[0].length() - 1); // remove o ultimo caractere
                    labels.add(label);
                    labelsAdresses.add(objProgram.size());
                }
                else {
                    // senao, deve ser uma variavel
                    variables.add(tokens[0]);
                }
            }
        }
    }


    // processa um comando, colocando ele e os parametros
    // (se existirem), no final da array
    protected void proccessCommand(String[] tokens) {
        String command = tokens[0];
        String parameter ="";
        String parameter2 = "";
        int commandNumber = findCommandNumber(tokens);
        if (commandNumber == 0) { // processar um comando add
            parameter = tokens[1];
            parameter = "&"+parameter; // uma flag para indicar que é uma posiçao na memoria
        }
        if (commandNumber == 1) { // processar um comando sub
            parameter = tokens[1];
            parameter = "&"+parameter;//this is a flag to indicate that is a position in memory
        }
        if (commandNumber == 2) { // processa um jmp
            parameter = tokens[1];
            parameter = "&"+parameter;// uma flag para indicar que é uma posiçao na memoria
        }
        if (commandNumber == 3) { // processa um jz
            parameter = tokens[1];
            parameter = "&"+parameter;// uma flag para indicar que é uma posiçao na memoria
        }
        if (commandNumber == 4) { // processa um jn
            parameter = tokens[1];
            parameter = "&"+parameter;// uma flag para indicar que é uma posiçao na memoria
        }
        if (commandNumber == 5) { // processa um read
            parameter = tokens[1];
            parameter = "&"+parameter;// uma flag para indicar que é uma posiçao na memoria
        }
        if (commandNumber == 6) { // processa um store
            parameter = tokens[1];
            parameter = "&"+parameter;// uma flag para indicar que é uma posiçao na memoria
        }
        if (commandNumber == 7) { // processa um ldi
            parameter = tokens[1];
        }
        if (commandNumber == 8) { // processa um inc

        }
        if (commandNumber == 9) { // processa um moveRegReg
            parameter = tokens[1];
            parameter2 = tokens[2];
        }
        objProgram.add(Integer.toString(commandNumber));
        if (!parameter.isEmpty()) {
            objProgram.add(parameter);
        }
        if (!parameter2.isEmpty()) {
            objProgram.add(parameter2);
        }
    }

    // o metodo a seguir usa os tokens para procurar um comando
    // na lista de comandos e retornar o id dele.
    private int findCommandNumber(String[] token) {
        int p = commands.indexOf(tokens[0]);

        // se o comando nao estiver na lista, entao deve ter multiplos formatos
        if(p < 0) {
            if ("move".equals(tokens[0]))
                p = proccessMove(tokens);
        }
        return p;
    }

    // esse metodo processa um comando de move
    // ele deve ter diferentes formatos, ou seja, diferentes
    // comandos internos
    private int proccessMove(String[] tokens) {
        String p1 = tokens[1];
        String p2 = tokens[2];
        int p=-1;
        if ((p1.startsWith("%"))&&(p2.startsWith("%"))) { //this is a moveRegReg comand
            p = commands.indexOf("moveRegReg");
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
        if (!checkLabels())
            return;
        execProgram = (ArrayList<String>) objProgram.clone();
        replaceAllVariables();
        replaceLabels(); // substituindo todos os labels pelos endereços que eles se referem
        replaceRegisters(); // substituindo todos os registradores pelo id de registrador que eles se referem
        saveExecFile(filename);
        System.out.println("Finished");
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

    // esse metodo substitui todas as labels no programa executavel
    // pelos endereços correspondentes as quais eles referem.
    protected void replaceLabels() {
        int i=0;
        for (String label : labels) { // analisando todas as labels
            label = "&"+label;
            int labelPointTo = labelsAdresses.get(i);
            int lineNumber = 0;
            for (String l : execProgram) {
                if (l.equals(label)) {// essa label precisa ser substituida pelo endereço
                    String newLine = Integer.toString(labelPointTo); // o endereço
                    execProgram.set(lineNumber, newLine);
                }
                lineNumber++;
            }
            i++;
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

    // esse metodo confere se todas as labels e as variaveis no
    // objProgram estavam no source.
    // As coleçoes de labels e das variaveis sao usadas para isso.
    protected boolean checkLabels() {
        System.out.println("Checking labels and variables");
        for (String line:objProgram) {
            boolean found = false;
            if (line.startsWith("&")) { //if starts with "&", it is a label or a variable
                line = line.substring(1, line.length());
                if (labels.contains(line))
                    found = true;
                if (variables.contains(line))
                    found = true;
                if (!found) {
                    System.out.println("FATAL ERROR! Variable or label "+line+" not declared!");
                    return false;
                }
            }
        }
        return true;
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
