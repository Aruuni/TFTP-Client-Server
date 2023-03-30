import java.io.IOException;
import java.net.*;
import java.util.Arrays;

public class UDPSocketServer extends Thread {
    protected DatagramPacket packet;
    protected DatagramSocket socket;
    /**
     * Constructor for the UDPSocketServer class
     *
     * @throws IOException
     */
    public UDPSocketServer() throws IOException {
        InetAddress inetAddress = InetAddress.getLocalHost();
        System.out.println("Local IP address: " + inetAddress.getHostAddress());
        socket = new DatagramSocket(9000, inetAddress);
    }
    /**
     * This method is responsible for receiving the initial packet from the client
     * It then creates a new thread to handle the rest of the communication
     */
    @Override
    public void run() {
        try {
            while (true) {
                //parse the request packet
                byte[] recvBuf = new byte[265];
                StringBuilder filename = new StringBuilder();
                packet = new DatagramPacket(recvBuf, recvBuf.length);
                try {
                    socket.receive(packet);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                recvBuf = packet.getData();
                //parrse the filename
                int currentByte = 2;
                while(recvBuf[currentByte] != 0){
                    filename.append((char) recvBuf[currentByte]);
                    currentByte++;
                }
                //Open a new thread to handle the rest of the communication
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
