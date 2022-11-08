import java.util.ArrayList;

public class Function {
    private String funcName;
    private String returnType;
    private ArrayList<Integer> paras;

    public Function(String funcName, String returnType) {
        this.funcName = funcName;
        this.returnType = returnType;
    }

    public String getFuncName() {
        return funcName;
    }

    public void setFuncName(String funcName) {
        this.funcName = funcName;
    }

    public String getReturnType() {
        return returnType;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    public ArrayList<Integer> getParas() {
        return paras;
    }

    public void setParas(ArrayList<Integer> paras) {
        this.paras = paras;
    }
}
