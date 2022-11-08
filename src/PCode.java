
public class PCode {
    private CodeType codeType;
    private String v1 = "";
    private String v2 = "";

    @Override
    public String toString() {
        if (codeType.equals(CodeType.FUNC)) {
            return "FUNC " + v1 + ":";
        }
        if (codeType.equals(CodeType.LABEL)) {
            return v1 + ":";
        }
        if (codeType.equals(CodeType.PRINT)) {
            return codeType + " " + v1;
        }
        if (codeType.equals(CodeType.CALL)) {
            return "call :" + v1;
        }
        return codeType + " " + v1 + ", " + v2;
    }

    public PCode(CodeType codeType) {
        this.codeType = codeType;
    }

    public PCode(CodeType codeType, String v1) {
        this.codeType = codeType;
        this.v1 = v1;
    }

    public PCode(CodeType codeType, String v1, String v2) {
        this.codeType = codeType;
        this.v1 = v1;
        this.v2 = v2;
    }

    public CodeType getCodeType() {
        return codeType;
    }

    public void setCodeType(CodeType codeType) {
        this.codeType = codeType;
    }

    public String getV1() {
        return v1;
    }

    public void setV1(String v1) {
        this.v1 = v1;
    }

    public String getV2() {
        return v2;
    }

    public void setV2(String v2) {
        this.v2 = v2;
    }
}
