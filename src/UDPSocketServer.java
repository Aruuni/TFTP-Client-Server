import java.io.IOException;
import java.net.*;
import java.util.Arrays;

public class UDPSocketServer extends Thread {
    protected DatagramPacket packet;
    protected DatagramSocket socket = null;
    public UDPSocketServer() throws IOException {
        InetAddress inetAddress = InetAddress.getLocalHost();
        System.out.println("Local IP address: " + inetAddress.getHostAddress());
        socket = new DatagramSocket(9000, inetAddress);
    }

    @Override
    public void run() {
        try {
            while (true) {
                byte[] recvBuf = new byte[256];
                StringBuilder filename = new StringBuilder();
                packet = new DatagramPacket(recvBuf, recvBuf.length);
                try {
                    socket.receive(packet);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                recvBuf = packet.getData();
                int currentByte = 2;
                while(recvBuf[currentByte] != 0){
                    filename.append((char) recvBuf[currentByte]);
                    currentByte++;
                }
                //reandom port
                new ServerRequestHandler(9001, packet, filename.toString(), recvBuf[1]).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            socket.close();
        }
    }
    //main method that starts the server
    public static void main(String[] args) throws IOException {
        new UDPSocketServer().start();
        System.out.println("Time Server Started");
    }

}
