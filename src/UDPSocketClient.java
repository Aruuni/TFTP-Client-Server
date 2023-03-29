
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.*;
import java.util.Arrays;

public class UDPSocketClient {
    private DatagramSocket socket;
    public DatagramPacket packet = new DatagramPacket(new byte[516], 516);
    private final int PORT = 4000;
    //change, make a random socket
    public UDPSocketClient() throws SocketException, UnknownHostException {
        socket = new DatagramSocket(PORT);
    }

    public void readHandler(String filename) throws IOException {
        this.sendRRQ(filename, 9000, InetAddress.getByName("192.168.68.119"));
        int blockNumber = 0;
        try (FileOutputStream fos = new FileOutputStream(filename)) {
            while (true) {
                try {
                    System.out.println("waiting for data");
                    socket.receive(packet);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                byte[] rcvBuf = packet.getData();
                System.out.println(blockNumber + "  " + ((rcvBuf[3]&0xff)|((rcvBuf[2]&0xff) << 8)));
                if (((rcvBuf[3] & 0xff)|((rcvBuf[2]&0xff) << 8)) == blockNumber + 1) {
                    fos.write(rcvBuf, 4, packet.getLength() - 4);
                    System.out.println(((rcvBuf[3]&0xff)|((rcvBuf[2]) >> 8)));
                    sendAck(((rcvBuf[3]&0xff)|((rcvBuf[2]&0xff) << 8)), packet);
                    blockNumber++;
                }
                if (packet.getLength() < 516) {
                    break; // last packet received
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendRRQ(String filename, int PORT, InetAddress addres) throws IOException {
        byte[] Opcode = new byte[2];
        Opcode[1] = 1;
        byte[] fileName = filename.getBytes();
        byte[] Mode = "octet".getBytes();
        byte[] request = new byte[265];
        System.arraycopy(Opcode, 0, request, 0, Opcode.length);
        System.arraycopy(fileName, 0, request, Opcode.length, fileName.length);
        System.arraycopy(Mode, 0, request, Opcode.length + fileName.length + 1, Mode.length);
        DatagramPacket rrqPacket = new DatagramPacket(request, 265, addres, PORT);
        System.out.println(Arrays.toString(request));
        socket.send(rrqPacket);
    }
    public void sendWRQ(String filename) throws IOException {
        byte[] Opcode = new byte[2];
        Opcode[1] = 2;
        byte[] Filename = "text.txt".getBytes();
        byte[] Mode = "octet".getBytes();
        byte[] request = new byte[Opcode.length + Filename.length + Mode.length + 2];
        System.arraycopy(Opcode, 0, request, 0, Opcode.length);
        System.arraycopy(Filename, 0, request, Opcode.length, Filename.length);
        System.arraycopy(Mode, 0, request, Opcode.length + Filename.length + 1, Mode.length);
        //packet = new DatagramPacket(request, request.length, InetAddress.getLocalHost(), 9000);
        System.out.println(request.length);
        //packet.setData(request);
    }
    public void sendAck(int block, DatagramPacket toAck) throws IOException {
        byte[] ack = new byte[4];
        ack[1] = 4;
        ack[2] = (byte) ((block>>8) & 0xff);
        ack[3] = (byte) (block&0xff);
        DatagramPacket ackPacket = new DatagramPacket(ack, 4, toAck.getAddress(), toAck.getPort());
        System.out.println(Arrays.toString(ackPacket.getData()));
        System.out.println(Arrays.toString(ack));
        System.out.println("Client sending ack " + block + " decoded: " + ((ack[3]&0xff)|((ack[2]&0xff) << 8)));
        socket.send(ackPacket);
    }
    public static void main(String[] args) throws IOException {


        System.out.println("Welcome to the TFTP client");
        System.out.println("Press 1 for write request");
        System.out.println("Press 2 for read request");
        while(true){
            int next = System.in.read();
            if (next == 49){
                System.out.println("Write request");
                UDPSocketClient client = new UDPSocketClient();
                client.readHandler("test.jpg");
                continue;
            }
             if (next == 50){
                System.out.println("Read request");
                break;
            }
            else{
                System.out.println("Invalid input");
            }
            System.out.println();
        }
    }
}
