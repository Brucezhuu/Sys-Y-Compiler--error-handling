public class lableGenerator {
    private int count = 0;

    public String generate(String type) {
        count++;
        return type + "_"+ count;
    }
}
