import java.util.ArrayList;

public class syntaxParser {
    private final ArrayList<Token> tokens; // 词法分析得到的 TokenList,用于语法分析
    private final ArrayList<String> res; // 最终返回的syntaxList

    private Token curToken;

    private int pos; //指向curToken在tokens中的位置
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
        while (TokenForwardIs("CONSTTK",0) && TokenForwardIs("INTTK", 1) && TokenForwardIs("IDENFR", 2) ||
                TokenForwardIs("INTTK",0) && TokenForwardIs("IDENFR", 1) && TokenForwardIs("LBRACK", 2) ||
                TokenForwardIs("INTTK",0) && TokenForwardIs("IDENFR", 1) && TokenForwardIs("ASSIGN", 2) ||
                TokenForwardIs("INTTK",0) && TokenForwardIs("IDENFR", 1) && TokenForwardIs("COMMA", 2) ||
                TokenForwardIs("INTTK",0) && TokenForwardIs("IDENFR", 1) && TokenForwardIs("SEMICN", 2)
        ) {
            parseDecl();
        }
        while(TokenForwardIs("IDENFR", 1) && TokenForwardIs("LPARENT", 2)
                && (TokenForwardIs("INTTK",0) || TokenForwardIs("VOIDTK",0))) {
            parseFuncDef();
        }
        parseMainFuncDef();
        res.add("<CompUnit>");
    }


    private void parseFuncDef() {
        parseFuncType();
        parseIdent();
        syntaxCheck("LPARENT");
        if (TokenForwardIs("RPARENT",0)) {
            peek();
        }
        else if (TokenForwardIs("LBRACE",0)) {
            error();
        }
        else {
            parseFuncFParams();
            syntaxCheck("RPARENT");
        }
        parseBlock();
        res.add("<FuncDef>");
    }

    private void parseFuncFParams() {
        parseFuncFParam();
        while(TokenForwardIs("COMMA",0)) {
            peek();
            parseFuncFParam();
        }
        res.add("<FuncFParams>");
    }

    private void parseFuncFParam() {
        parseBtype();
        parseIdent();
        if(TokenForwardIs("LBRACK",0)) {
            peek();
            syntaxCheck("RBRACK");
            while(TokenForwardIs("LBRACK",0)) {
                peek();
                parseConstExp();
                syntaxCheck("RBRACK");
            }
        }
        res.add("<FuncFParam>");
    }

    private void parseFuncType() {
        if (TokenForwardIs("VOIDTK",0) || TokenForwardIs("INTTK",0)) {
            peek();
            res.add("<FuncType>");
        } else {
            error();
        }
    }
    private void parseMainFuncDef() {
        syntaxCheck("INTTK");
        syntaxCheck("MAINTK");
        syntaxCheck("LPARENT");
        syntaxCheck("RPARENT");
        parseBlock();
        res.add("<MainFuncDef>");
    }

    private void parseBlock() {
        syntaxCheck("LBRACE");
        while(!TokenForwardIs("RBRACE",0)) {
            parseBlockItem();
        }
        syntaxCheck("RBRACE");
        res.add("<Block>");
    }

    private void parseBlockItem() {
        if (TokenForwardIs("CONSTTK",0) || TokenForwardIs("INTTK",0)) {
            parseDecl();
        }
        else {
            parseStmt();
        }
    }

    private void parseStmt() {
        if (TokenForwardIs("IFTK",0)) {
            peek();
            syntaxCheck("LPARENT");
            parseCond();
            syntaxCheck("RPARENT");
            parseStmt();
            if (TokenForwardIs("ELSETK",0)) {
                peek();
                parseStmt();
            }
        }
        else if (TokenForwardIs("WHILETK",0)) {
            peek();
            syntaxCheck("LPARENT");
            parseCond();
            syntaxCheck("RPARENT");
            parseStmt();
        }
        else if(TokenForwardIs("BREAKTK",0) || TokenForwardIs("CONTINUETK",0)) {
            peek();
            syntaxCheck("SEMICN");
        }
        else if (TokenForwardIs("RETURNTK",0)) {
            peek();
            if (TokenForwardIs("SEMICN",0)) {
                peek();
            }
            else if (TokenForwardIs("RBRACE",0)) {
                error();
            }
            else {
                parseExp();
                syntaxCheck("SEMICN");
            }
        }
        else if (TokenForwardIs("PRINTFTK",0)) {
            peek();
            syntaxCheck("LPARENT");

            parseFormatString();
            while(TokenForwardIs("COMMA",0)) {
                peek();
                parseExp();
            }
            syntaxCheck("RPARENT");
            syntaxCheck("SEMICN");
        }
        else if (TokenForwardIs("LBRACE",0)) {
            parseBlock();
        }
        else if (TokenForwardIs("IDENFR",0) && isLVal()) {
            parseLVal();
            syntaxCheck("ASSIGN");
            if (TokenForwardIs("GETINTTK",0)) {
                peek();
                syntaxCheck("LPARENT");
                syntaxCheck("RPARENT");
                syntaxCheck("SEMICN");
            }
            else {
                parseExp();
                syntaxCheck("SEMICN");
            }
        }
        else {
            if (TokenForwardIs("SEMICN",0)) {
                peek();
            }
            else {
                parseExp();
                syntaxCheck("SEMICN");
            }
        }
        res.add("<Stmt>");
    }

    private boolean isLVal() {
        int i = 1;
        while (pos + i < tokens.size()) {
            Token newToken = tokens.get(i + pos);
            if (newToken.getValue().equals(";")) {
                break;
            }
            else if (newToken.getValue().equals("=")) {
                return true;
            }
            i++;
        }
        return false;
    }

    private void parseFormatString() {
        if (!TokenForwardIs("STRCON",0)) error();
        peek();
    }

    private void parseCond() {
        parseLOrExp();
        res.add("<Cond>");
    }

    private void parseLOrExp() {
        parseLAndExp();
        while(TokenForwardIs("OR",0)) {
            res.add("<LOrExp>");
            peek();
            parseLAndExp();
        }
        res.add("<LOrExp>");
    }

    private void parseLAndExp() {
        parseEqExp();
        while (TokenForwardIs("AND",0)) {
            res.add("<LAndExp>");
            peek();
            parseEqExp();
        }
        res.add("<LAndExp>");
    }

    private void parseEqExp() {
        parseRelExp();
        while (TokenForwardIs("EQL",0) || TokenForwardIs("NEQ",0)) {
            res.add("<EqExp>");
            peek();
            parseRelExp();
        }
        res.add("<EqExp>");
    }

    private void parseRelExp() {
        parseAddExp();
        while (TokenForwardIs("LSS",0) || TokenForwardIs("GRE",0)
                || TokenForwardIs("LEQ",0) || TokenForwardIs("GEQ",0)) {
            res.add("<RelExp>");
            peek();
            parseAddExp();
        }
        res.add("<RelExp>");
    }

    private void parseDecl() {
        if (curToken.getValue().equals("const")) {
            parseConstDecl();
        } else {
            parseVarDecl();
        }
    }

    private void parseConstDecl(){
        if (!curToken.getValue().equals("const")) {
            error();
        }
        if (peek()) {
            parseBtype();
            parseConstDef();
            while (TokenForwardIs("COMMA",0)) {
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
        while (TokenForwardIs("COMMA",0)) {
            if (peek()) {
                parseVarDef();
            }
        }
        syntaxCheck("SEMICN");
        res.add("<VarDecl>");
    }

    private void parseVarDef() {
        parseIdent();
        boolean isArray = false;
        int dimension = 0;
        while (TokenForwardIs("LBRACK",0)) {
            if (peek()) {
                parseConstExp();
                syntaxCheck("RBRACK");
                isArray = true;
                dimension++;
            }
        }
        if (TokenForwardIs("ASSIGN",0)) {
            peek();
            parseInitVal();
        }
        res.add("<VarDef>");
    }

    private void parseConstDef() {
        parseIdent();
        boolean isArray = false;
        int dimension = 0;
        while (TokenForwardIs("LBRACK",0)) {
            if (peek()) {
                parseConstExp();
                syntaxCheck("RBRACK");
                isArray = true;
                dimension++;
            }
        }
        syntaxCheck("ASSIGN");
        parseConstInitVal();
        res.add("<ConstDef>");
    }

    private void parseConstExp() {
        parseAddExp();
        res.add("<ConstExp>");
    }

    private void parseConstInitVal() {
        if (TokenForwardIs("LBRACE",0)) {
            peek();
            if (TokenForwardIs("RBRACE",0)) {
                peek();
            } else {
                parseConstInitVal();
                while (TokenForwardIs("COMMA",0)) {
                    peek();
                    parseConstInitVal();
                }
                syntaxCheck("RBRACE");
            }
        }
        else {
            parseConstExp();
        }
        res.add("<ConstInitVal>");
    }

    private void parseInitVal() {
        if (TokenForwardIs("LBRACE",0)) {
            peek();
            if (TokenForwardIs("RBRACE",0)) {
                peek();
            } else {
                parseInitVal();
                while (TokenForwardIs("COMMA",0)) {
                    peek();
                    parseInitVal();
                }
                syntaxCheck("RBRACE");
            }
        }
        else {
            parseExp();
        }
        res.add("<InitVal>");
    }

    private void parseExp() {
        parseAddExp();
        res.add("<Exp>");
    }

    private void parseAddExp() {
        parseMulExp();
        while(TokenForwardIs("PLUS",0) || TokenForwardIs("MINU",0)) {
            res.add("<AddExp>");
            peek();
            parseMulExp();
        }
        res.add("<AddExp>");
    }

    private void parseMulExp() {
        parseUnaryExp();
        while (TokenForwardIs("MULT",0) || TokenForwardIs("DIV",0) || TokenForwardIs("MOD",0)) {
            res.add("<MulExp>");
            peek();
            parseUnaryExp();
        }
        res.add("<MulExp>");
    }

    private void parseUnaryExp() {
        if (TokenForwardIs("IDENFR",0) && TokenForwardIs("LPARENT",1)) {
            peek();peek();
            if (TokenForwardIs("RPARENT",0)) {
                peek();
            } else if (TokenForwardIs("RBRACK",0)) {
                error();
            } else {
                parseFuncRParams();
                syntaxCheck("RPARENT");
            }
        }
        else if (TokenForwardIs("PLUS",0) || TokenForwardIs("MINU",0) || TokenForwardIs("NOT",0)) {
            parseUnaryOp();
            parseUnaryExp();
        }
        else {
            parsePrimaryExp();
        }
        res.add("<UnaryExp>");
    }


    private void parsePrimaryExp() {
        if (TokenForwardIs("LPARENT",0)) {
            peek();
            parseExp();
            syntaxCheck("RPARENT");
        }
        else if (TokenForwardIs("IDENFR",0)) {
            parseLVal();
        }
        else if (TokenForwardIs("INTCON",0)) {
            parseNumber();
        }
        else {
            error();
        }
        res.add("<PrimaryExp>");
    }

    private void parseNumber() {
        syntaxCheck("INTCON");
        res.add("<Number>");
    }

    private void parseLVal() {
        parseIdent();
        while (TokenForwardIs("LBRACK",0)) {
            peek();
            parseExp();
            syntaxCheck("RBRACK");
        }
        res.add("<LVal>");
    }

    private void parseUnaryOp() {
        if (TokenForwardIs("PLUS",0) || TokenForwardIs("MINU",0) || TokenForwardIs("NOT",0)) {
            peek();
        } else {
            error();
        }
        res.add("<UnaryOp>");
    }

    private void parseFuncRParams() {
        parseExp();
        while(TokenForwardIs("COMMA",0)) {
            peek();
            parseExp();
        }
        res.add("<FuncRParams>");
    }

    private void parseBtype() {
        syntaxCheck("INTTK");
    }

    private void parseIdent() {
        syntaxCheck("IDENFR");
    }

    private Boolean peek() { //获取下一个Token改变，修改pos和curToken
        res.add(curToken.toString());
        if (pos < tokens.size() - 1) {
            curToken = tokens.get(++pos);
            return true;
        }
        else {
            return false; //tokens已经读完
        }
    }

    private Boolean TokenForwardIs(String sym,int offset) { //向前offset读Token
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
        if (TokenForwardIs(sym,0)) {
            peek();
        } else {
            error();
        }
    }
    private void error() {
        System.out.println("error at" + curToken.toString());
        System.exit(0);
    }
}
