public class Symbol {
    private int scopeID;

    private String type;

    private String content;

    private int dim; // 0:int; 1:one dim array; 2: two dim array;

    @Override
    public String toString() {
        return content;
    }

    public Symbol(int scopeID, String type, String content, int dim) {
        this.scopeID = scopeID;
        this.type = type;
        this.content = content;
        this.dim = dim;
    }

    public int getScopeID() {
        return scopeID;
    }

    public void setScopeID(int scopeID) {
        this.scopeID = scopeID;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getDim() {
        return dim;
    }

    public void setDim(int dim) {
        this.dim = dim;
    }
}
