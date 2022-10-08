import java.io.IOException;
import java.util.ArrayList;

public class Compiler {
    public static void main(String[] args) throws IOException {
        FileManipulate f = new FileManipulate();
        ArrayList<String> lines = f.readFile();
        ArrayList<Token> tokens;
        Lexer lexer = new Lexer(lines);
        tokens = lexer.analyze();
        /// f.writeFile(res);
        syntaxParser par = new syntaxParser(tokens);
        ArrayList<String> res = par.parse();
        f.writeFile(res);
    }
}