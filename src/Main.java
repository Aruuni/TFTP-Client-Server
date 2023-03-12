import java.net.DatagramPacket;

public class Main {
    public static void main(String[] args) {

        DatagramPacket packet = new DatagramPacket(new byte[8], 8 );
        System.out.println("Hello world!");

    }
}