public class Token {
    private String key;
    private String value;

    private int row; //行号
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public int getRow() {
        return row;
    }

    public void setRow(int row) {
        this.row = row;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Token(String key, String value,int row) {
        this.key = key;
        this.value = value;
        this.row = row;
    }

    @Override
    public String toString() {
        return this.key + ' ' + this.value;
    }
}


