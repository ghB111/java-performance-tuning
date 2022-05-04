package nsu.fit.javaperf.lab2;

public class Program2 {
    public static void main(String[] args) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < 10000; ++i) {
            s.append(" ").append(String.valueOf(i));
        }
        System.out.println(s.length());
    }
}
