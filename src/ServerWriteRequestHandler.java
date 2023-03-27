import java.io.IOException;
import java.net.*;

public class ServerWriteRequestHandler extends Thread {

    private DatagramSocket socket;
    private DatagramPacket packet;
    private int blockNumber = 0;

    public ServerWriteRequestHandler(DatagramSocket socket, DatagramPacket packet, String filename) {
        this.socket = socket;
        this.packet = packet;
    }
    public void Ack(){
        byte[] Opcode = new byte[2];
        Opcode[1] = 4;
        byte[] BlockNumber = new byte[2];
        BlockNumber[0] = (byte) (blockNumber >> 8);
        BlockNumber[1] = (byte) (blockNumber);
        byte[] ackResponsePacket = new byte[Opcode.length + BlockNumber.length];
        System.arraycopy(Opcode, 0, ackResponsePacket, 0, Opcode.length);
        System.arraycopy(BlockNumber, 0, ackResponsePacket, Opcode.length, BlockNumber.length);
        packet = new DatagramPacket(ackResponsePacket, ackResponsePacket.length, packet.getAddress(), packet.getPort());
        try {
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        try {
            System.out.println("Waiting for ack ...");
            socket.receive(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            String request = new String(packet.getData(), 0, packet.getLength());
            System.out.println("Handling request: " + request);


            String response = "Hello, client!";
            byte[] data = response.getBytes();
            DatagramPacket responsePacket = new DatagramPacket(data, data.length, packet.getAddress(), packet.getPort());
            socket.send(responsePacket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}