

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class syntaxParser {
    private final ArrayList<Token> tokens; // 词法分析得到的 TokenList,用于语法分析
    private final ArrayList<String> res; // 最终返回的syntaxList

    private Token curToken;

    //    private int scope = -1;
    private int curScope = -1;
    private int scope = -1;
    private HashMap<Integer, symbolTable> symbolTables = new HashMap<>();
    private ArrayList<Error> errors = new ArrayList<>();
    private ArrayList<PCode> codes = new ArrayList<>();
    private HashMap<String, Function> functions = new HashMap<String, Function>();
    private ArrayList<HashMap<String, String>> ifLabels = new ArrayList<>();
    private ArrayList<HashMap<String, String>> whileLabels = new ArrayList<>();
    private lableGenerator labGen = new lableGenerator();
    private int pos; //指向curToken在tokens中的位置
    private int whileFlag = 0; // means if the current code block is in while circle.

    private boolean needRetVal = false;

    public syntaxParser(ArrayList<Token> tokens) {
        this.tokens = tokens;
        this.pos = 0;
        this.curToken = tokens.size() == 0 ? null : tokens.get(0);
        this.res = new ArrayList<>();
    }

    public ArrayList<String> parse() {
        parseCompUnit();
        return res;
    }

    private void parseCompUnit() {
        addAndEnterScope();
        while (TokenForwardIs("CONSTTK", 0) && TokenForwardIs("INTTK", 1) && TokenForwardIs("IDENFR", 2) ||
                TokenForwardIs("INTTK", 0) && TokenForwardIs("IDENFR", 1) && TokenForwardIs("LBRACK", 2) ||
                TokenForwardIs("INTTK", 0) && TokenForwardIs("IDENFR", 1) && TokenForwardIs("ASSIGN", 2) ||
                TokenForwardIs("INTTK", 0) && TokenForwardIs("IDENFR", 1) && TokenForwardIs("COMMA", 2) ||
                TokenForwardIs("INTTK", 0) && TokenForwardIs("IDENFR", 1) && TokenForwardIs("SEMICN", 2)
        ) {
            parseDecl();
        }
        while (TokenForwardIs("IDENFR", 1) && TokenForwardIs("LPARENT", 2)
                && (TokenForwardIs("INTTK", 0) || TokenForwardIs("VOIDTK", 0))) {
            parseFuncDef();
        }
        if (TokenForwardIs("INTTK", 0) && TokenForwardIs("MAINTK", 1)) {
            parseMainFuncDef();
        } else {
            error();
        }
        removeLastScope();
        res.add("<CompUnit>");
    }


    private void parseFuncDef() {
        String returnType = parseFuncType();
        ArrayList<Integer> paras = new ArrayList<>();
        Token ident = curToken;
        parseIdent();
        if (functions.containsKey(ident.getValue())) {
            error("b", ident.getRow());
        }
        if (symbolTables.get(0).hasSymbol(ident)) {
            error("b", ident.getRow());
        }
        PCode code = new PCode(CodeType.FUNC, ident.getValue());
        codes.add(code);
        Function function = new Function(ident.getValue(), returnType);
        addAndEnterScope();
        syntaxCheck("LPARENT");
        if (TokenForwardIs("RPARENT", 0)) {
            peek();
        } else if (TokenForwardIs("LBRACE", 0)) {
            error("j",tokens.get(pos - 1).getRow());
        } else {
            paras = parseFuncFParams();
            syntaxCheck("RPARENT");
        }
        function.setParas(paras);
        functions.put(function.getFuncName(), function);
        boolean isRetVal = parseBlock(true);
        if (needRetVal && !isRetVal) {
            error("g", tokens.get(pos - 1).getRow());
        }
        removeLastScope();
        code.setV2(String.valueOf(paras.size()));
        codes.add(new PCode(CodeType.RET, "" + 0));
        codes.add(new PCode(CodeType.ENDFUNC));
        res.add("<FuncDef>");
    }

    private ArrayList<Integer> parseFuncFParams() {
        ArrayList<Integer> paras = new ArrayList<>();
        int paraType = parseFuncFParam();
        paras.add(paraType);
        while (TokenForwardIs("COMMA", 0)) {
            peek();
            paraType = parseFuncFParam();
            paras.add(paraType);
        }
        res.add("<FuncFParams>");
        return paras;
    }

    private int parseFuncFParam() {
        parseBtype();
        Token ident = curToken;
        parseIdent();
        if (duplicatedSymbol(ident)) {
            error("b", ident.getRow());
        }
        int paraType = 0;
        if (TokenForwardIs("LBRACK", 0)) {
            peek();
            paraType++;
            syntaxCheck("RBRACK");
            while (TokenForwardIs("LBRACK", 0)) {
                paraType++;
                peek();
                parseConstExp();
                syntaxCheck("RBRACK");
            }
        }
        codes.add(new PCode(CodeType.PARA, curScope + "_" + ident.getValue(), "" + paraType));
        symbolTables.get(scope).addSymbol(new Symbol(curScope, "para", ident.getValue(), paraType));
        res.add("<FuncFParam>");
        return paraType;
    }

    private String parseFuncType() {
        if (TokenForwardIs("VOIDTK", 0) || TokenForwardIs("INTTK", 0)) {
            needRetVal = TokenForwardIs("INTTK", 0);
            Token token = curToken;
            peek();
            res.add("<FuncType>");
            return token.getValue();
        } else {
            error();
            return null;
        }
    }

    private void parseMainFuncDef() {
        syntaxCheck("INTTK");
        if (functions.containsKey(curToken.getValue()) || symbolTables.get(0).hasSymbol(curToken)) {
            error("b", curToken.getRow());
        } else {
            Function function = new Function(curToken.getValue(), "int");
            function.setParas(new ArrayList<>());
            functions.put("main", function);
        }
        codes.add(new PCode(CodeType.MAIN, curToken.getValue()));
        needRetVal = true;
        syntaxCheck("MAINTK");
        syntaxCheck("LPARENT");
        syntaxCheck("RPARENT");
        boolean isRetVal = parseBlock(false);
        if (needRetVal && !isRetVal) {
            error("g", tokens.get(pos - 1).getRow());
        }
        codes.add(new PCode(CodeType.EXIT));
        res.add("<MainFuncDef>");
    }

    private boolean parseBlock(boolean isFuncDef) {
        syntaxCheck("LBRACE");
        if (!isFuncDef) {
            addAndEnterScope();
        }
        boolean isReturn = false;
        while (!TokenForwardIs("RBRACE", 0)) {
            isReturn = parseBlockItem();
        }
        Token rbrace = curToken;
        syntaxCheck("RBRACE");
        res.add("<Block>");
        if (!isFuncDef) {
            removeLastScope();
        }
        return isReturn;
    }

    private boolean parseBlockItem() {
        boolean isReturn = false;
        if (TokenForwardIs("CONSTTK", 0) || TokenForwardIs("INTTK", 0)) {
            parseDecl();
        } else {
            isReturn = parseStmt();
        }
        return isReturn;
    }

    private boolean parseStmt() {
        boolean isRetVal = false;
        if (TokenForwardIs("IFTK", 0)) {
            ifLabels.add(new HashMap<>());
            ifLabels.get(ifLabels.size() - 1).put("if", labGen.generate("if"));
            ifLabels.get(ifLabels.size() - 1).put("else", labGen.generate("else"));
            ifLabels.get(ifLabels.size() - 1).put("if_end", labGen.generate("if_end"));
            ifLabels.get(ifLabels.size() - 1).put("if_block", labGen.generate("if_block"));
            codes.add(new PCode(CodeType.LABEL, ifLabels.get(ifLabels.size() - 1).get("if")));
            peek();
            syntaxCheck("LPARENT");
            parseCond("IFTK");
            syntaxCheck("RPARENT");
            codes.add(new PCode(CodeType.JZ, ifLabels.get(ifLabels.size() - 1).get("else")));
            codes.add(new PCode(CodeType.LABEL, ifLabels.get(ifLabels.size() - 1).get("if_block")));
            parseStmt();
            codes.add(new PCode(CodeType.JMP, ifLabels.get(ifLabels.size() - 1).get("if_end")));
            codes.add(new PCode(CodeType.LABEL, ifLabels.get(ifLabels.size() - 1).get("else")));
            if (TokenForwardIs("ELSETK", 0)) {
                peek();
                parseStmt();
            }
            codes.add(new PCode(CodeType.LABEL, ifLabels.get(ifLabels.size() - 1).get("if_end")));
            ifLabels.remove(ifLabels.size() - 1);
        } else if (TokenForwardIs("WHILETK", 0)) {
            whileLabels.add(new HashMap<>());
            whileLabels.get(whileLabels.size() - 1).put("while", labGen.generate("while"));
            whileLabels.get(whileLabels.size() - 1).put("while_end", labGen.generate("while_end"));
            whileLabels.get(whileLabels.size() - 1).put("while_block", labGen.generate("while_block"));
            codes.add(new PCode(CodeType.LABEL, whileLabels.get(whileLabels.size() - 1).get("while")));
            peek();
            whileFlag++;
            syntaxCheck("LPARENT");
            parseCond("WHILETK");
            syntaxCheck("RPARENT");
            codes.add(new PCode(CodeType.JZ, whileLabels.get(whileLabels.size() - 1).get("while_end")));
            codes.add(new PCode(CodeType.LABEL, whileLabels.get(whileLabels.size() - 1).get("while_block")));
            parseStmt();
            whileFlag--;
            codes.add(new PCode(CodeType.JMP, whileLabels.get(whileLabels.size() - 1).get("while")));
            codes.add(new PCode(CodeType.LABEL, whileLabels.get(whileLabels.size() - 1).get("while_end")));
            whileLabels.remove(whileLabels.size() - 1);
        } else if (TokenForwardIs("BREAKTK", 0) || TokenForwardIs("CONTINUETK", 0)) {
            if (TokenForwardIs("BREAKTK", 0)) {
                if (whileFlag == 0) {
                    error("m",curToken.getRow());
                }
                else {
                    codes.add(new PCode(CodeType.JMP, whileLabels.get(whileLabels.size() - 1).get("while_end")));
                }
            } else {
                if (whileFlag == 0) {
                    error("m",curToken.getRow());
                } else {
                    codes.add(new PCode(CodeType.JMP, whileLabels.get(whileLabels.size() - 1).get("while")));
                }
            }
            peek();
            syntaxCheck("SEMICN");
        } else if (TokenForwardIs("RETURNTK", 0)) {
            Token ret = curToken;
            peek();
            if (TokenForwardIs("SEMICN", 0)) {
                peek();
            } else if (TokenForwardIs("RBRACE", 0)) {
                error("i",tokens.get(pos - 1).getRow());
            } else {
                parseExp();
                syntaxCheck("SEMICN");
                isRetVal = true;
                if (!needRetVal) {
                    error("f", ret.getRow());
                }
            }
            codes.add(new PCode(CodeType.RET, String.valueOf(isRetVal ? 1 : 0)));
        } else if (TokenForwardIs("PRINTFTK", 0)) {
            Token printTk = curToken;
            peek();
            syntaxCheck("LPARENT");
            Token strtk = parseFormatString();
            if (illegalForStr(strtk.getValue())) {
                error("a", strtk.getRow());
            }
            int para = 0;
            while (TokenForwardIs("COMMA", 0)) {
                peek();
                parseExp();
                para++;
            }
            if (para != getFormatNum(strtk)) {
                error("l", printTk.getRow());
            }
            syntaxCheck("RPARENT");
            syntaxCheck("SEMICN");
            codes.add(new PCode(CodeType.PRINT, strtk.getValue(), "" + para));
        } else if (TokenForwardIs("LBRACE", 0)) {
            parseBlock(false);
        } else if (TokenForwardIs("IDENFR", 0) && isLVal()) {
            Token ident = curToken;
            int dim = parseLVal();
            if (isConst(ident)) {
                error("h", ident.getRow());
            }
            if (hasSymbol(ident)) {
                codes.add(new PCode(CodeType.ADDRESS, getSymbol(ident).getScopeID() + "_" + ident.getValue(), String.valueOf(dim)));
            }
            syntaxCheck("ASSIGN");
            if (TokenForwardIs("GETINTTK", 0)) {
                peek();
                syntaxCheck("LPARENT");
                syntaxCheck("RPARENT");
                syntaxCheck("SEMICN");
                codes.add(new PCode(CodeType.GETINT));
            } else {
                parseExp();
                syntaxCheck("SEMICN");
            }
            if (hasSymbol(ident)) {
                codes.add(new PCode(CodeType.ASSIGN, getSymbol(ident).getScopeID() + "_" + ident.getValue()));
            }
        } else {
            if (TokenForwardIs("SEMICN", 0)) {
                peek();
            } else {
                parseExp();
                syntaxCheck("SEMICN");
            }
        }
        res.add("<Stmt>");
        return isRetVal;
    }


    private int getFormatNum(Token strtk) {
        String str = strtk.getValue();
        int ans = 0;
        for (int i = 1 ;i < str.length() - 1;i++) {
            if (str.charAt(i) == '%' && str.charAt(i + 1) == 'd') {
                ans++;
            }
        }
        return ans;
    }

    private boolean isConst(Token t) {
//        symbolTable curSymbolTable = symbolTables.get(scope);
//        if (curSymbolTable.hasSymbol(t)) {
//            return curSymbolTable.getSymbol(t).getType().equals("const");
//        }

//        for (symbolTable st : symbolTables.values()) {
//            if (st.hasSymbol(t)) {
//                return st.getSymbol(t).getType().equals("const");
//            }
//        }
        for (int i = scope; i >= 0; i--) {
            symbolTable st = symbolTables.get(i);
            if (st.hasSymbol(t)) {
                return st.getSymbol(t).getType().equals("const");
            }
        }
        return false;
    }

    private boolean isLVal() {
        int i = 1;
        while (pos + i < tokens.size()) {
            Token newToken = tokens.get(i + pos);
            if (newToken.getValue().equals(";") || newToken.getRow() > curToken.getRow()) {
                break;
            } else if (newToken.getValue().equals("=")) {
                return true;
            }
            i++;
        }
        return false;
    }

    private Token parseFormatString() {
        if (!TokenForwardIs("STRCON", 0)) error();
        Token strcon = curToken;
        peek();
        return strcon;
    }

    private void parseCond(String from) {
        parseLOrExp(from);
        res.add("<Cond>");
    }

    private void parseLOrExp(String from) {
        int cnt = 0;
        String label = labGen.generate("cond_" + cnt);
        cnt++;
        parseLAndExp(from, label);
        codes.add(new PCode(CodeType.LABEL, label));
        if (from.equals("IFTK")) {
            codes.add(new PCode(CodeType.JNZ, ifLabels.get(ifLabels.size() - 1).get("if_block")));
        } else {
            codes.add(new PCode(CodeType.JNZ, whileLabels.get(whileLabels.size() - 1).get("while_block")));
        }
        while (TokenForwardIs("OR", 0)) {
            res.add("<LOrExp>");
            peek();
            label = labGen.generate("cond_" + cnt);
            cnt++;
            parseLAndExp(from, label);
            codes.add(new PCode(CodeType.LABEL, label));
            codes.add(new PCode(CodeType.OR));
            if (from.equals("IFTK")) {
                codes.add(new PCode(CodeType.JNZ, ifLabels.get(ifLabels.size() - 1).get("if_block")));
            } else {
                codes.add(new PCode(CodeType.JNZ, whileLabels.get(whileLabels.size() - 1).get("while_block")));
            }
        }
        res.add("<LOrExp>");
    }

    private void parseLAndExp(String from, String label) {
        parseEqExp();
        if (from.equals("IFTK")) {
            codes.add(new PCode(CodeType.JZ, label));
        } else {
            codes.add(new PCode(CodeType.JZ, label));
        }
        while (TokenForwardIs("AND", 0)) {
            res.add("<LAndExp>");
            peek();
            parseEqExp();
            codes.add(new PCode(CodeType.AND));
            if (from.equals("IFTK")) {
                codes.add(new PCode(CodeType.JZ, label));
            } else {
                codes.add(new PCode(CodeType.JZ, label));
            }
        }
        res.add("<LAndExp>");
    }

    private void parseEqExp() {
        parseRelExp();
        while (TokenForwardIs("EQL", 0) || TokenForwardIs("NEQ", 0)) {
            res.add("<EqExp>");
            Token op = curToken;
            peek();
            parseRelExp();
            CodeType type;
            if (op.getKey().equals("EQL")) {
                type = CodeType.EQ;
            } else {
                type = CodeType.NEQ;
            }
            codes.add(new PCode(type));
        }
        res.add("<EqExp>");
    }

    private void parseRelExp() {
        parseAddExp();
        while (TokenForwardIs("LSS", 0) || TokenForwardIs("GRE", 0)
                || TokenForwardIs("LEQ", 0) || TokenForwardIs("GEQ", 0)) {
            res.add("<RelExp>");
            Token op = curToken;
            peek();
            parseAddExp();
            CodeType type;
            if (op.getKey().equals("LSS")) {
                type = CodeType.LT;
            } else if (op.getKey().equals("LEQ")) {
                type = CodeType.LE;
            } else if (op.getKey().equals("GRE")) {
                type = CodeType.GT;
            } else {
                type = CodeType.GE;
            }
            codes.add(new PCode(type));
        }
        res.add("<RelExp>");
    }

    private void parseDecl() {
        if (curToken.getValue().equals("const")) {
            parseConstDecl();
        } else if (curToken.getValue().equals("int")) {
            parseVarDecl();
        } else {
            error();
        }
    }

    private void parseConstDecl() {
        if (!curToken.getValue().equals("const")) {
            error();
        }
        if (peek()) {
            parseBtype();
            parseConstDef();
            while (TokenForwardIs("COMMA", 0)) {
                peek();
                parseConstDef();
            }
            syntaxCheck("SEMICN");
            res.add("<ConstDecl>");
        }

    }

    private void parseVarDecl() {
        parseBtype();
        parseVarDef();
        while (TokenForwardIs("COMMA", 0)) {
            if (peek()) {
                parseVarDef();
            }
        }
        syntaxCheck("SEMICN");
        res.add("<VarDecl>");
    }

    private void parseVarDef() {
        Token ident = curToken;
        parseIdent();
        if (duplicatedSymbol(ident)) {
            error("b", ident.getRow());
        }
        int dimension = 0;
        codes.add(new PCode(CodeType.VAR, curScope + "_" + ident.getValue()));
        while (TokenForwardIs("LBRACK", 0)) {
            if (peek()) {
                dimension++;
                parseConstExp();
                syntaxCheck("RBRACK");
            }
        }
        if (dimension > 0) {
            codes.add(new PCode(CodeType.ARRAY, curScope + "_" + ident.getValue(), String.valueOf(dimension)));
        }
        symbolTables.get(scope).addSymbol(new Symbol(curScope, "var", ident.getValue(), dimension));

        if (TokenForwardIs("ASSIGN", 0)) {
            peek();
            parseInitVal();
        } else {
            codes.add(new PCode(CodeType.SETZERO, curScope + "_" + ident.getValue(), String.valueOf(dimension)));
        }
        res.add("<VarDef>");
    }

    private void parseConstDef() {
        Token ident = curToken;
        parseIdent();
        if (duplicatedSymbol(ident)) {
            error("b", ident.getRow());
        }
        codes.add(new PCode(CodeType.VAR, curScope + "_" + ident.getValue()));
        int dimension = 0;
        while (TokenForwardIs("LBRACK", 0)) {
            if (peek()) {
                parseConstExp();
                syntaxCheck("RBRACK");
                dimension++;
            }
        }
        if (dimension > 0) {
            codes.add(new PCode(CodeType.ARRAY, curScope + "_" + ident.getValue(), String.valueOf(dimension)));
        }
        symbolTables.get(scope).addSymbol(new Symbol(curScope, "const", ident.getValue(), dimension));
        syntaxCheck("ASSIGN");
        parseConstInitVal();
        res.add("<ConstDef>");
    }

    private void parseConstExp() {
        parseAddExp();
        res.add("<ConstExp>");
    }

    private void parseConstInitVal() {
        if (TokenForwardIs("LBRACE", 0)) {
            peek();
            if (TokenForwardIs("RBRACE", 0)) {
                peek();
            } else {
                parseConstInitVal();
                while (TokenForwardIs("COMMA", 0)) {
                    peek();
                    parseConstInitVal();
                }
                syntaxCheck("RBRACE");
            }
        } else {
            parseConstExp();
        }
        res.add("<ConstInitVal>");
    }

    private void parseInitVal() {
        if (TokenForwardIs("LBRACE", 0)) {
            peek();
            if (TokenForwardIs("RBRACE", 0)) {
                peek();
            } else {
                parseInitVal();
                while (TokenForwardIs("COMMA", 0)) {
                    peek();
                    parseInitVal();
                }
                syntaxCheck("RBRACE");
            }
        } else {
            parseExp();
        }
        res.add("<InitVal>");
    }

    private int parseExp() {
        int dim = parseAddExp();
        res.add("<Exp>");
        return dim;
    }

    private int parseAddExp() {
        int dim = parseMulExp();
        while (TokenForwardIs("PLUS", 0) || TokenForwardIs("MINU", 0)) {
            res.add("<AddExp>");
            Token op = curToken;
            peek();
            dim = parseMulExp();
            if (op.getKey().equals("PLUS")) {
                codes.add(new PCode(CodeType.ADD));
            } else {
                codes.add(new PCode(CodeType.SUB));
            }
        }
        res.add("<AddExp>");
        return dim;
    }

    private int parseMulExp() {
        int dim = parseUnaryExp();
        while (TokenForwardIs("MULT", 0) || TokenForwardIs("DIV", 0) || TokenForwardIs("MOD", 0)) {
            Token op = curToken;
            res.add("<MulExp>");
            peek();
            dim = parseUnaryExp();
            if (op.getKey().equals("MULT")) {
                codes.add(new PCode(CodeType.MUL));
            } else if (op.getKey().equals("DIV")) {
                codes.add(new PCode(CodeType.DIV));
            } else {
                codes.add(new PCode(CodeType.MOD));
            }
        }
        res.add("<MulExp>");
        return dim;
    }

    private int parseUnaryExp() {
        int dim = 0;
        if (TokenForwardIs("IDENFR", 0) && TokenForwardIs("LPARENT", 1)) {
            Token ident = curToken;
            ArrayList<Integer> paras = null;
            if (!hasFunction(ident)) {
                error("c", ident.getRow());
            } else {
                paras = getFunction(ident).getParas();
            }
            peek();
            peek();
            if (TokenForwardIs("RPARENT", 0)) {
                peek();
                if (paras != null && paras.size() != 0) {
                    error("d", ident.getRow());
                }
            } else if (TokenForwardIs("SEMICN", 0)) {
                error("j", tokens.get(pos - 1).getRow());
            }
            else {
                parseFuncRParams(paras, ident);
                syntaxCheck("RPARENT");
            }
            codes.add(new PCode(CodeType.CALL, ident.getValue()));
            if (hasFunction(ident)) {
                if (getFunction(ident).getReturnType().equals("void")) {
                    dim = -1;
                }
            }
        } else if (TokenForwardIs("PLUS", 0) || TokenForwardIs("MINU", 0) || TokenForwardIs("NOT", 0)) {
            Token t = curToken;
            parseUnaryOp();
            parseUnaryExp();
            if (t.getKey().equals("PLUS")) {
                codes.add(new PCode(CodeType.POS));
            } else if (t.getKey().equals("MINU")) {
                codes.add(new PCode(CodeType.NEG));
            } else {
                codes.add(new PCode(CodeType.NOT));
            }
        } else {
            dim = parsePrimaryExp();
        }
        res.add("<UnaryExp>");
        return dim;
    }


    private int parsePrimaryExp() {
        int dim = 0;
        if (TokenForwardIs("LPARENT", 0)) {
            peek();
            parseExp();
            syntaxCheck("RPARENT");
        } else if (TokenForwardIs("IDENFR", 0)) {
            Token ident = curToken;
            dim = parseLVal();
            int trueDim = -1;
            if (hasSymbol(ident)) {
                trueDim = getSymbol(ident).getDim();
            }
            if (trueDim != -1) {
                dim = trueDim - dim;
            } else {
                dim = 0;
            }
            if (dim == 0 && trueDim != -1) {
                codes.add(new PCode(CodeType.VALUE, getSymbol(ident).getScopeID() + "_" + ident.getValue(), String.valueOf(trueDim - dim)));
            } else {
                if (trueDim != -1) {
                    codes.add(new PCode(CodeType.ADDRESS, getSymbol(ident).getScopeID() + "_" + ident.getValue(), String.valueOf(trueDim - dim)));
                }
            }
        } else if (TokenForwardIs("INTCON", 0)) {
            parseNumber(curToken);
        } else {
            error();
        }
        res.add("<PrimaryExp>");
        return dim;
    }

    private void parseNumber(Token token) {
        syntaxCheck("INTCON");
        codes.add(new PCode(CodeType.PUSH, String.valueOf(token.getValue())));
        res.add("<Number>");
    }

    private int parseLVal() {
        Token ident = curToken;
        int dim = 0;
        parseIdent();
        if (!hasSymbol(ident)) {
            error("c", ident.getRow());
        }
        while (TokenForwardIs("LBRACK", 0)) {
            dim++;
            peek();
            parseExp();
            syntaxCheck("RBRACK");
        }
        res.add("<LVal>");
        return dim;
    }

    private void parseUnaryOp() {
        if (TokenForwardIs("PLUS", 0) || TokenForwardIs("MINU", 0) || TokenForwardIs("NOT", 0)) {
            peek();
        } else {
            error();
        }
        res.add("<UnaryOp>");
    }

    private void parseFuncRParams(ArrayList<Integer> paras, Token func) {
        int dim = parseExp();
        ArrayList<Integer> rParams = new ArrayList<>();
        rParams.add(dim);
        codes.add(new PCode(CodeType.RPARA, String.valueOf(dim)));
        while (TokenForwardIs("COMMA", 0)) {
            peek();
            dim = parseExp();
            rParams.add(dim);
            codes.add(new PCode(CodeType.RPARA, String.valueOf(dim)));
        }
        if (paras != null) {
            checkParams(paras, rParams, func);
        }
        res.add("<FuncRParams>");
    }

    private void checkParams(ArrayList<Integer> params, ArrayList<Integer> rParams, Token func) {
        if (params.size() != rParams.size()) {
            error("d", func.getRow());
        } else {
            for (int i = 0; i < params.size(); i++) {
                if (!Objects.equals(rParams.get(i), params.get(i))) {
                    error("e", func.getRow());
                }
            }
        }
    }


    private void parseBtype() {
        syntaxCheck("INTTK");
    }

    private void parseIdent() {
        syntaxCheck("IDENFR");
    }

    public boolean illegalForStr(String s) {
        for (int i = 1; i < s.length() - 1; i++) {
            char c = s.charAt(i);
            if (!(c == 32 || c == 33 || (c >= 40 && c <= 126))) {
                if (c == '%' && i + 1 < s.length() && s.charAt(i + 1) == 'd') {
                    continue;
                }
                return true;
            } else {
                if (c == '\\' && i + 1 < s.length() && s.charAt(i + 1) != 'n') {
                    return true;
                }
            }
        }
        return false;
    }

    private Boolean peek() { //获取下一个Token改变，修改pos和curToken
        res.add(curToken.toString());
        if (pos < tokens.size() - 1) {
            curToken = tokens.get(++pos);
            return true;
        } else {
            pos++;
            return false; //tokens已经读完
        }
    }

    private Boolean TokenForwardIs(String sym, int offset) { //向前offset读Token
        if (pos + offset >= tokens.size()) {
            return false; //tokens已经读完
        } else {
            Token forward = tokens.get(pos + offset);
            return forward.getKey().equals(sym); //true : 下一个Token的sym与所给sym相同
        }
    }

    private Token getLastToken() { //获取当前token的上一个token
        if (pos == 0) {
            return tokens.get(0);
        } else {
            return tokens.get(pos - 1);
        }
    }

    private void syntaxCheck(String sym) {
        if (TokenForwardIs(sym, 0)) {
            peek();
        } else {
            switch (sym) {
                case "SEMICN":
                    error("i", tokens.get(pos - 1).getRow());
                    break;
                case "RPARENT":
                    error("j", tokens.get(pos - 1).getRow());
                    break;
                case "RBRACK":
                    error("k", tokens.get(pos - 1).getRow());
                    break;
                default:
                    error();
            }
        }
    }

    private void error() {
        System.out.println("error at" + curToken.toString());
        System.exit(0);
    }

    private void addAndEnterScope() {
        curScope++;
        scope++;
        symbolTables.put(scope, new symbolTable());
    }

    private void removeLastScope() {
        symbolTables.remove(scope);
        scope--;
    }

    private void error(String type, int lineNo) {
        errors.add(new Error(lineNo, type));
    }

    private boolean duplicatedSymbol(Token token) {
        return symbolTables.get(scope).hasSymbol(token);
    }

    private boolean hasFunction(Token token) {
        return functions.containsKey(token.getValue());
    }

    private Function getFunction(Token token) {
        return functions.getOrDefault(token.getValue(), null);
    }

    private Symbol getSymbol(Token token) {
        Symbol symbol = null;
        for (symbolTable s : symbolTables.values()) {
            if (s.hasSymbol(token)) {
                symbol = s.getSymbol(token);
            }
        }
        return symbol;
    }

    public ArrayList<Token> getTokens() {
        return tokens;
    }

    public ArrayList<String> getRes() {
        return res;
    }

    public Token getCurToken() {
        return curToken;
    }

    public void setCurToken(Token curToken) {
        this.curToken = curToken;
    }

    public int getCurScope() {
        return curScope;
    }

    public void setCurScope(int curScope) {
        this.curScope = curScope;
    }

    public ArrayList<Error> getErrors() {
        return errors;
    }

    public void setErrors(ArrayList<Error> errors) {
        this.errors = errors;
    }

    public ArrayList<PCode> getCodes() {
        return codes;
    }

    public void setCodes(ArrayList<PCode> codes) {
        this.codes = codes;
    }

    public HashMap<String, Function> getFunctions() {
        return functions;
    }

    public void setFunctions(HashMap<String, Function> functions) {
        this.functions = functions;
    }

    public ArrayList<HashMap<String, String>> getIfLabels() {
        return ifLabels;
    }

    public void setIfLabels(ArrayList<HashMap<String, String>> ifLabels) {
        this.ifLabels = ifLabels;
    }

    public ArrayList<HashMap<String, String>> getWhileLabels() {
        return whileLabels;
    }

    public void setWhileLabels(ArrayList<HashMap<String, String>> whileLabels) {
        this.whileLabels = whileLabels;
    }

    public lableGenerator getLabGen() {
        return labGen;
    }

    public void setLabGen(lableGenerator labGen) {
        this.labGen = labGen;
    }

    public int getPos() {
        return pos;
    }

    public void setPos(int pos) {
        this.pos = pos;
    }

    public int getWhileFlag() {
        return whileFlag;
    }

    public void setWhileFlag(int whileFlag) {
        this.whileFlag = whileFlag;
    }

    public boolean hasSymbol(Token token) {
        for (symbolTable st : symbolTables.values()) {
            if (st.hasSymbol(token)) {
                return true;
            }
        }
        return false;
    }
}
