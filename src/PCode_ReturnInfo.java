import java.util.HashMap;

public class PCode_ReturnInfo {
    private int returnPc;
    private int stackTop;
    private HashMap<String, Variable> varTable;

    private int totalParaNum;
    private int curParaNum;

    public int getTotalParaNum() {
        return totalParaNum;
    }

    public int getCurParaNum() {
        return curParaNum;
    }

    public PCode_ReturnInfo(int returnPc, int stackTop, HashMap<String, Variable> varTable, int totalParaNum, int curParaNum) {
        this.returnPc = returnPc;
        this.stackTop = stackTop;
        this.varTable = varTable;
        this.totalParaNum = totalParaNum;
        this.curParaNum = curParaNum;
    }

    public int getReturnPc() {
        return returnPc;
    }

    public void setReturnPc(int returnPc) {
        this.returnPc = returnPc;
    }

    public int getStackTop() {
        return stackTop;
    }

    public void setStackTop(int stackTop) {
        this.stackTop = stackTop;
    }

    public HashMap<String, Variable> getVarTable() {
        return varTable;
    }

    public void setVarTable(HashMap<String, Variable> varTable) {
        this.varTable = varTable;
    }
}
