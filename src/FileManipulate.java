import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

public class FileManipulate {
    private File inFile = new File("./testfile.txt");

    public ArrayList<String> readFile() {
        FileReader fr = null;
        BufferedReader bufr = null;
        ArrayList<String> ans = new ArrayList<>();
        try {
            fr = new FileReader(inFile);
            bufr = new BufferedReader(fr);
            String str = null;
            while ((str = bufr.readLine()) != null) {
                String tmp = new String(str);
                ans.add(tmp);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ans;
    }

    public void writeFile(ArrayList<String> res) throws IOException {
        String outStream = ".//output.txt";
        PrintStream printStream = new PrintStream(Files.newOutputStream(Paths.get(outStream)));
        System.setOut(printStream);
        for (String s : res) {
            System.out.println(s);
        }
    }

    public void writePcodeResult(ArrayList<String> res) throws IOException {
        FileWriter writer = new FileWriter("pcoderesult.txt");
        for (String s : res) {
            writer.write(s);
        }
        writer.flush();
        writer.close();
    }
}