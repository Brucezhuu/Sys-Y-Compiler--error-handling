import java.util.ArrayList;
import java.util.HashMap;

public class Lexer {
    private ArrayList<String> lines;
    private HashMap<String, String> reserves = new HashMap<>(); // token 2 sym
    private int pos = 0;
    private ArrayList<Token> res = new ArrayList<>(); // sym 2 token
    private boolean quomark = false;
    private boolean notemark = false;
    private StringBuilder formatstr;

    public Lexer(ArrayList<String> lines) {
        this.lines = lines;
        reserves.put("printf", "PRINTFTK");
        reserves.put("return", "RETURNTK");
        reserves.put("while", "WHILETK");
        reserves.put("getint", "GETINTTK");
        reserves.put("main", "MAINTK");
        reserves.put("const", "CONSTTK");
        reserves.put("int", "INTTK");
        reserves.put("break", "BREAKTK");
        reserves.put("continue", "CONTINUETK");
        reserves.put("if", "IFTK");
        reserves.put("else", "ELSETK");
        reserves.put("void", "VOIDTK");
    }

    private Boolean analyzeOneLine(String line, int row) {
        while (pos < line.length()) {
            char c = line.charAt(pos);
            if (Character.isWhitespace(c) && !quomark) {
                while (Character.isWhitespace(c) && pos < line.length()) {
                    c = line.charAt(pos++);
                }
                if (!Character.isWhitespace(c)) {
                    pos--;
                } else {
                    return true;
                }
            }
            if (notemark) {
                while (pos < line.length() && !(line.charAt(pos) == '*' && pos + 1 < line.length() && line.charAt(pos + 1) == '/')) {
                    pos++;
                }
                if (pos >= line.length()) {
                    return true;
                } else {
                    notemark = false;
                    pos += 2;
                    continue;
                }
            }
            if (c == '/') {
                if (pos + 1 < line.length()) {
                    if (line.charAt(pos + 1) == '/') {
                        return true;
                    } else if (line.charAt(pos + 1) == '*') {
                        notemark = true;
                        pos += 2;
                        while (pos < line.length() && !(line.charAt(pos) == '*' && pos + 1 < line.length() && line.charAt(pos + 1) == '/')) {
                            pos++;
                        }
                        if (pos >= line.length()) {
                            return true;
                        } else {
                            notemark = false;
                            pos += 2;
                            continue;
                        }
                    }
                }
            }

            StringBuilder sb = new StringBuilder();
            if (!quomark && c == '\"') {
                quomark = true;
                pos++;
                formatstr = new StringBuilder();
                formatstr.append('\"');
                while (pos < line.length() && line.charAt(pos) != '\"') {
                    formatstr.append(line.charAt(pos));
                    pos++;
                }
                if (pos >= line.length()) {
                    formatstr.append('\n');
                    return true;
                } else {
                    formatstr.append('\"');
                    quomark = false;
                    res.add(new Token("STRCON", formatstr.toString(), row));
                    formatstr = new StringBuilder();
                    pos++;
                }
            } else if (quomark) {
                while (pos < line.length() && line.charAt(pos) != '\"') {
                    formatstr.append(line.charAt(pos));
                    pos++;
                }
                if (pos >= line.length()) {
                    formatstr.append('\n');
                    return true;
                } else {
                    formatstr.append('\"');
                    quomark = false;
                    res.add(new Token("STRCON", formatstr.toString(), row));
                    formatstr = new StringBuilder();
                    pos++;
                }
            } else if (isLetter(c)) {
                while (isLetter(c) || isDigit(c)) {
                    sb.append(c);
                    if (pos + 1 < line.length()) {
                        pos++;
                        c = line.charAt(pos);
                    } else {
                        pos++;
                        break;
                    }
                }
                String token = sb.toString();
                String sym;
                sym = reserves.getOrDefault(token, "IDENFR");
                res.add(new Token(sym, token, row));

            } else if (isDigit(c)) {
                while (isDigit(c)) {
                    sb.append(c);
                    if (pos + 1 < line.length()) {
                        pos++;
                        c = line.charAt(pos);
                    } else {
                        pos++;
                        break;
                    }
                }
                String sym = "INTCON";
                String token = sb.toString();

                res.add(new Token(sym, token, row));
            } else if (c == '!') {
                if (pos + 1 < line.length() && line.charAt(pos + 1) == '=') {
                    pos++;
                    res.add(new Token("NEQ", "!=", row));
                } else {
                    res.add(new Token("NOT", "!", row));
                }
                pos++;
            } else if (c == '+') {
                res.add(new Token("PLUS", "+", row));
                pos++;
            } else if (c == '-') {
                res.add(new Token("MINU", "-", row));
                pos++;
            } else if (c == '*') {
                res.add(new Token("MULT", "*", row));
                pos++;
            } else if (c == '/') {
                res.add(new Token("DIV", "/", row));
                pos++;
            } else if (c == '=') {
                if (pos + 1 < line.length() && line.charAt(pos + 1) == '=') {
                    pos++;
                    res.add(new Token("EQL", "==", row));
                } else {
                    res.add(new Token("ASSIGN", "=", row));
                }
                pos++;
            } else if (c == ';') {
                res.add(new Token("SEMICN", ";", row));
                pos++;
            } else if (c == ',') {
                res.add(new Token("COMMA", ",", row));
                pos++;
            } else if (c == '(') {
                res.add(new Token("LPARENT", "(", row));
                pos++;
            } else if (c == ')') {
                res.add(new Token("RPARENT", ")", row));
                pos++;
            } else if (c == '[') {
                res.add(new Token("LBRACK", "[", row));
                pos++;
            } else if (c == ']') {
                res.add(new Token("RBRACK", "]", row));
                pos++;
            } else if (c == '{') {
                res.add(new Token("LBRACE", "{", row));
                pos++;
            } else if (c == '}') {
                res.add(new Token("RBRACE", "}", row));
                pos++;
            } else if (c == '%') {
                res.add(new Token("MOD", "%", row));
                pos++;
            } else if (c == '<') {
                if (pos + 1 < line.length() && line.charAt(pos + 1) == '=') {
                    pos++;
                    res.add(new Token("LEQ", "<=", row));
                } else {
                    res.add(new Token("LSS", "<", row));
                }
                pos++;
            } else if (c == '>') {
                if (pos + 1 < line.length() && line.charAt(pos + 1) == '=') {
                    pos++;
                    res.add(new Token("GEQ", ">=", row));
                } else {
                    res.add(new Token("GRE", ">", row));
                }
                pos++;
            } else if (c == '&') {
                if (pos + 1 < line.length() && line.charAt(pos + 1) == '&') {
                    pos += 2;
                    res.add(new Token("AND", "&&", row));
                } else {
                    return Boolean.FALSE;
                }
            } else if (c == '|') {
                if (pos + 1 < line.length() && line.charAt(pos + 1) == '|') {
                    pos += 2;
                    res.add(new Token("OR", "||", row));
                } else {
                    return Boolean.FALSE;
                }
            } else {
                return Boolean.FALSE;
            }
        }
        return Boolean.TRUE;
    }

    public ArrayList<Token> analyze() {
        HashMap<String, String> res = new HashMap<>();
        for (int i = 0; i < lines.size(); i++) {
            String str = lines.get(i);
            this.pos = 0;
            if (analyzeOneLine(str, i + 1) == Boolean.FALSE) {
                error(i);
            }
        }
        return this.res;
    }

    private void error(int lineno) {
        System.out.println("Error! in line " + lineno);
    }

    private Boolean isLetter(char c) {
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c == '_');
    }

    private Boolean isDigit(char c) {
        return (c <= '9' && c >= '0');
    }

}
