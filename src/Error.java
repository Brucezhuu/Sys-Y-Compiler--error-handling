public class Error implements Comparable {
    private int n;
    private String type;

    private String errorInfo;

    public Error(int n, String type) {
        this.n = n;
        this.type = type;
    }

    public int getN() {
        return n;
    }

    public void setN(int n) {
        this.n = n;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getErrorInfo() {
        return errorInfo;
    }

    public void setErrorInfo(String errorInfo) {
        this.errorInfo = errorInfo;
    }

    @Override
    public String toString() {
        return n + " " + type;
    }

    @Override
    public int compareTo(Object o) {
        Error e = (Error) o;
        if (this.n > e.n) {
            return 1;
        } else {
            return -1;
        }
    }
}
