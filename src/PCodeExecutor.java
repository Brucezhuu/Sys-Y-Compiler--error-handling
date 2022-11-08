import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Scanner;

public class PCodeExecutor {
    /*
    `funcTable`: memorizes the address of the function in codes list.
     */
    private ArrayList<PCode> codes;
    private int pc = 0;
    private HashMap<String,Variable> name2Var = new HashMap<String, Variable>();
    private ArrayList<Integer> stack = new ArrayList<>();

    private int mainAddress = -1;
    private ArrayList<PCode_ReturnInfo> returnInfos = new ArrayList<>();
    private boolean inMain = false;
    private int curPara = 0;
    private int totalParas = 0;

    private ArrayList<Integer> rParas = new ArrayList<>();
    private HashMap<String, Integer> lab2Index = new HashMap<>();
    private HashMap<String, PCode_Func> name2Func = new HashMap<>();
    private ArrayList<String> printList = new ArrayList<>();
    Scanner sc = new Scanner(System.in);
    public PCodeExecutor(ArrayList<PCode> codes) {

        this.codes = codes;
        for (int i = 0;i < codes.size();i ++) {
            PCode code = codes.get(i);
            if (code.getCodeType() == CodeType.LABEL) {
                lab2Index.put(code.getV1(), i);
            }
            if (code.getCodeType() == CodeType.MAIN) {
                mainAddress = i;
            }
            if (code.getCodeType() == CodeType.FUNC) {
                name2Func.put(code.getV1(), new PCode_Func(i, Integer.parseInt(code.getV2())));
            }
        }
    }

    public void run() {
        for (pc = 0;pc < codes.size();pc ++) {
            PCode code = codes.get(pc);
            switch (code.getCodeType()) {
                case VAR: {
                    Variable var = new Variable(stack.size());
                    name2Var.put(code.getV1(), var);
                }
                break;
                case PUSH: {
                    push(Integer.valueOf(code.getV1()));
                }
                break;
                case ASSIGN: {
                    int value = pop();
                    int address = pop();
                    stack.set(address, value);
                }
                break;
                case POS:{
                    push(pop());
                }
                    break;
                case JZ: {
                    if (stack.get(stack.size() - 1) == 0) {
                        pc = lab2Index.get(code.getV1());
                    }
                }
                break;
                case JNZ: {
                    if (stack.get(stack.size() - 1) != 0) {
                        pc = lab2Index.get(code.getV1());
                    }
                }
                break;
                case JMP: {
                    pc = lab2Index.get(code.getV1());
                }
                break;
                case GETINT: {
                    int x = sc.nextInt();
                    push(x);
                }
                break;
                case PRINT: {
                    String str = code.getV1();
                    int paraNum = Integer.parseInt(code.getV2());
                    ArrayList<Integer> paras = new ArrayList<>();
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0;i < paraNum;i ++) {
                        paras.add(pop());
                    }
                    Collections.reverse(paras);
                    for (int i = 0,j = 0;i < str.length();i ++) {
                        if (str.charAt(i) == '%' && i + 1 < str.length() && str.charAt(i + 1) == 'd') {
                            sb.append(paras.get(j++));
                            i++;
                        }
                        else if (str.charAt(i) == '\\' && i + 1 < str.length() && str.charAt(i + 1) == 'n') {
                            sb.append('\n');
                            i++;
                        }
                        else {
                            sb.append(str.charAt(i));
                        }
                    }
                    printList.add(sb.substring(1, sb.length() - 1));
                }
                break;
                case VALUE:{
                    Variable var = getVar(code.getV1());
                    int dim = Integer.parseInt(code.getV2());
                    int address = getAddress(var, dim);
                    push(stack.get(address));
                }
                break;
                case ADDRESS:{
                    Variable var = getVar(code.getV1());
                    int dim = Integer.parseInt(code.getV2());
                    int address = getAddress(var,dim);
                    push(address);
                }
                break;
                case ARRAY:{
                    Variable var = getVar(code.getV1());
                    int dim = Integer.parseInt(code.getV2());
                    var.setDimension(dim);
                    if (dim == 1) {
                        int i = pop();
                        var.setDim1(i);
                    }
                    else if (dim == 2) {
                        int j = pop();
                        int i = pop();
                        var.setDim1(i);
                        var.setDim2(j);
                    }
                }
                break;
                case SETZERO: {
                    Variable var = getVar(code.getV1());
                    int dim = var.getDimension();
                    if (dim == 0) {
                        push(0);
                    }
                    if (dim == 1) {
                        int n = var.getDim1();
                        for (int i = 0;i < n;i ++) {
                            push(0);
                        }
                    }
                    if (dim == 2) {
                        int n = var.getDim1();
                        int m = var.getDim2();
                        for (int i = 0;i < n * m;i ++) {
                            push(0);
                        }
                    }
                }
                break;
                case MAIN: {
                    inMain = false;
                    returnInfos.add(new PCode_ReturnInfo(codes.size(), stack.size() - 1, name2Var,0, 0));
                    name2Var = new HashMap<>();
                }
                break;
                case FUNC: {
                    if (!inMain) {
                        pc = mainAddress - 1;
                    }
                }
                break;
                case PARA:{
                    Variable para = new Variable(rParas.get(rParas.size() - totalParas + curPara));
                    int dim = Integer.parseInt(code.getV2());
                    para.setDimension(dim);
                    if (dim == 2) {
                        para.setDim2(pop());
                    }
                    name2Var.put(code.getV1(), para);
                    curPara++;
                    if (curPara == totalParas) {
                        rParas.subList(rParas.size() - curPara, rParas.size()).clear();
                    }
                }
                break;
                case RPARA: {
                    int dim = Integer.parseInt(code.getV1());
                    if (dim == 0) {
                        rParas.add(stack.size() - 1);
                    } else {
                        rParas.add(stack.get(stack.size() - 1));
                    }
                }
                break;
                case RET: {
                    int f = Integer.parseInt(code.getV1());
                    PCode_ReturnInfo returnInfo = returnInfos.remove(returnInfos.size() - 1);
                    pc = returnInfo.getReturnPc();
                    name2Var = returnInfo.getVarTable();
                    curPara = returnInfo.getCurParaNum();
                    totalParas = returnInfo.getTotalParaNum();
                    if (f == 1) {
                        stack.subList(returnInfo.getStackTop() + 1 - returnInfo.getTotalParaNum(), stack.size() - 1).clear();
                    }
                    else {
                        stack.subList(returnInfo.getStackTop() + 1 - returnInfo.getTotalParaNum(), stack.size()).clear();
                    }
                }
                break;
                case CALL: {
                    PCode_Func func = name2Func.get(code.getV1());
                    returnInfos.add(new PCode_ReturnInfo(pc,stack.size() - 1 ,name2Var, func.getParaNum(), curPara));
                    pc = func.getIndex();
                    name2Var = new HashMap<>();
                    totalParas = func.getParaNum();
                    curPara = 0;
                }
                break;
                case ADD: {
                    int b = pop();
                    int a = pop();
                    push(a + b);
                }
                break;
                case SUB: {
                    int b = pop();
                    int a = pop();
                    push(a - b);
                }
                break;
                case MUL: {
                    int b = pop();
                    int a = pop();
                    push(a * b);
                }
                break;
                case DIV: {
                    int b = pop();
                    int a = pop();
                    push(a / b);
                }
                break;
                case MOD: {
                    int b = pop();
                    int a = pop();
                    push(a % b);
                }
                break;
                case EQ: {
                    int b = pop();
                    int a = pop();
                    push(a == b ? 1 : 0);
                }
                break;
                case NEQ: {
                    int b = pop();
                    int a = pop();
                    push(a != b ? 1 : 0);
                }
                break;
                case LT: {
                    int b = pop();
                    int a = pop();
                    push(a < b ? 1 : 0);
                }
                break;
                case LE: {
                    int b = pop();
                    int a = pop();
                    push(a <= b ? 1 : 0);
                }
                break;
                case GT: {
                    int b = pop();
                    int a = pop();
                    push(a > b ? 1 : 0);
                }
                break;
                case GE: {
                    int b = pop();
                    int a = pop();
                    push(a >= b ? 1 : 0);
                }
                break;
                case AND: {
                    boolean b = pop() != 0;
                    boolean a = pop() != 0;
                    push((a && b) ? 1 : 0);
                }
                break;
                case OR: {
                    boolean b = pop() != 0;
                    boolean a = pop() != 0;
                    push((a || b) ? 1 : 0);
                }
                break;
                case NOT: {
                    boolean a = pop() != 0;
                    push((!a) ? 1 : 0);
                }
                break;
                case NEG:{
                    push(-pop());
                }
                break;
                case EXIT: {
                    return;
                }
            }
        }
    }

    private int getAddress(Variable var, int dim) {
        int addr = 0;
        if (dim == 0) {
            addr = var.getIndex();
        }
        else if (dim == 1) {
            int i = pop();
            if (var.getDimension() == 1) {
                addr = var.getIndex() + i;
            }
            else {
                addr = var.getIndex() + var.getDim2() * i;
            }
        }
        else if (dim == 2) {
            int j = pop();
            int i = pop();
            addr = var.getIndex() + i * var.getDim2() + j;
        }
        return addr;
    }
    private void push(Integer a) {
        stack.add(a);
    }

    public ArrayList<String> getPrintList() {
        return printList;
    }

    private Integer pop() {
        return stack.remove(stack.size() - 1);
    }

    private Variable getVar(String ident) {
        if (name2Var.containsKey(ident)) {
            return name2Var.get(ident);
        }
        else {
            return returnInfos.get(0).getVarTable().get(ident); // 全局变量表
        }
    }
}
