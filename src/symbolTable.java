import java.util.HashMap;

public class symbolTable {
    private HashMap<String,Symbol> symbolMap = new HashMap<>();

    public void addSymbol(Symbol symbol) {
        symbolMap.put(symbol.getContent(), symbol);
    }

    public boolean hasSymbol(Token token) {
        return symbolMap.containsKey(token.getValue());
    }

    public Symbol getSymbol(Token token) {
        return symbolMap.get(token.getValue());
    }

    public boolean isConst(Token token) {
        return symbolMap.get(token.getValue()).getType().equals("const");
    }
}
