package nsu.fit.javaperf.lab2;

public class lab2 {
    public static void main(String[] args) {
        String s = "";
        for (int i = 0; i < 10000; ++i) {
            s += " " + String.valueOf(i);
        }
        System.out.println(s.length());
    }
}