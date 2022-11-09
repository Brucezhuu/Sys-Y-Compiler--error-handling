import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

public class Compiler {
    public static void main(String[] args) throws IOException {
        FileManipulate f = new FileManipulate();
        ArrayList<String> lines = f.readFile();
        ArrayList<Token> tokens;
        Lexer lexer = new Lexer(lines);
        tokens = lexer.analyze();
        syntaxParser parser = new syntaxParser(tokens);
        parser.parse();
        ArrayList<PCode> codes = parser.getCodes();
        ArrayList<Error> errors = parser.getErrors();
        if (errors.size() == 0) {
            PCodeExecutor executor = new PCodeExecutor(codes);
            executor.run();
            f.writePcodeResult(executor.getPrintList());
        }
        else{
            Collections.sort(errors);
            f.printErrors(errors);
        }
    }
}