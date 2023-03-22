import java.io.IOException;
import java.net.*;

public class ServerRequestHandler extends Thread {

    private DatagramSocket socket;
    private DatagramPacket packet;
    private int blockNumber = 0;
    //true == read, false == write
    public ServerRequestHandler(DatagramSocket socket, DatagramPacket packet, String filename, Boolean isRead) {
        this.socket = socket;
        this.packet = packet;
    }
    public void sendAck(int blockr){
        byte[] Opcode = new byte[2];
        Opcode[1] = 4;
        byte[] packetBlockNumber = new byte[2];
        packetBlockNumber[0] = (byte) (blockr >> 8);
        packetBlockNumber[1] = (byte) (blockr);
        byte[] request = new byte[Opcode.length + packetBlockNumber.length];
        System.arraycopy(Opcode, 0, request, 0, Opcode.length);
        System.arraycopy(packetBlockNumber, 0, request, Opcode.length, packetBlockNumber.length);
        packet.setData(request);
    }
    public void run() {
        try {
            System.out.println("Waiting for ack ...");
            socket.receive(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            // Handle the request
            String request = new String(packet.getData(), 0, packet.getLength());
            System.out.println("Handling request: " + request);

            // Send the response
            String response = "Hello, client!";
            byte[] data = response.getBytes();
            DatagramPacket responsePacket = new DatagramPacket(data, data.length, packet.getAddress(), packet.getPort());
            socket.send(responsePacket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}