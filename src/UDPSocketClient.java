
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.Arrays;

public class UDPSocketClient {
    private DatagramSocket socket;
    private DatagramPacket packet = new DatagramPacket(new byte[516], 516);
    protected DatagramPacket ackPacket = new DatagramPacket(new byte[4], 4);

    private final int PORT = 4000;
    //change, make a random socket
    public UDPSocketClient() throws SocketException {
        socket = new DatagramSocket(PORT);
    }

    public void readHandler(String filename, int PORT, InetAddress address) throws IOException {
        this.request((byte)1, filename, PORT, address);
        int blockNumber = 0;
        try (FileOutputStream fos = new FileOutputStream(Paths.get("./ClientFiles/",filename).toFile())) {
            while (true) {
                try {
                    System.out.println("waiting for data");
                    socket.receive(packet);
                    socket.getSoTimeout();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                byte[] rcvBuf = packet.getData();
                System.out.println(Arrays.toString(rcvBuf));
                if (rcvBuf[1] == 5) {
                    System.out.println("Error code: " + rcvBuf[3] + " Error message: " + new String(rcvBuf, 4, packet.getLength() - 3));
                    break;
                }
                sendAck(((rcvBuf[3]&0xff)|((rcvBuf[2]&0xff) << 8)), packet);
                if (((rcvBuf[3] & 0xff)|((rcvBuf[2]&0xff) << 8)) == blockNumber + 1) {
                    fos.write(rcvBuf, 4, packet.getLength() - 4);
                    blockNumber++;
                }
                if (packet.getLength() < 516) {
                    break;
                }
            }
        } catch (IOException e) {
            return;
        }
        socket.close();
    }
    public void writeHandler(String filename, int PORT, InetAddress address) throws IOException {
        try {
            byte[] fileData = Files.readAllBytes(Paths.get("./ClientFiles/",filename));
            this.request((byte)2, filename, PORT, address);
            System.out.println("Processing write request ..." + filename);
            int blockNumber = 0;
            try {

                int packetSize = 512;
                int numPackets = (int) Math.ceil((double) fileData.length / packetSize);
                if (fileData.length % packetSize == 0 && fileData.length != 0) {
                    numPackets++;
                }
                while (blockNumber < numPackets) {
                    try {
                        System.out.println("waiting for ACK" + blockNumber);
                        socket.receive(ackPacket);
                    } catch (IOException e) {
                        continue;
                    }
                    packet.setAddress(ackPacket.getAddress());
                    packet.setPort(ackPacket.getPort());
                    if (((ackPacket.getData()[3]&0xff)|((ackPacket.getData()[2]&0xff) << 8)) == blockNumber){
                        blockNumber++;
                    }
                    int startIndex = (blockNumber-1) * 512;
                    int endIndex = Math.min(startIndex + 512, fileData.length);
                    byte[] data = new byte[endIndex - startIndex + 4];
                    data[0] = 0;
                    data[1] = 3;
                    data[2] = (byte) ((blockNumber>>8)&0xff);
                    data[3] = (byte) ((blockNumber)&0xff);
                    System.arraycopy(Arrays.copyOfRange(fileData, startIndex, endIndex), 0, data, 4, endIndex - startIndex);
                    DatagramPacket dataPacket = new DatagramPacket(data, data.length, packet.getAddress(), packet.getPort());
                    if (packet.getLength() < 516) {
                        break;
                    }
                    socket.send(dataPacket);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                socket.close();
            }
            System.out.println("File sent successfully!");
        }
        catch  (NoSuchFileException e) {
            System.out.println("File not found");
        }
    }

    public void request(byte opcode,String filename, int PORT, InetAddress address) throws IOException {
        byte[] Opcode = new byte[2];
        Opcode[1] = opcode;
        byte[] fileName = filename.getBytes();
        byte[] Mode = "octet".getBytes();
        byte[] request = new byte[265];
        System.arraycopy(Opcode, 0, request, 0, Opcode.length);
        System.arraycopy(fileName, 0, request, Opcode.length, fileName.length);
        System.arraycopy(Mode, 0, request, Opcode.length + fileName.length + 1, Mode.length);
        DatagramPacket requestPacket = new DatagramPacket(request, 265, address, PORT);
        socket.send(requestPacket);
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
    // fix this
    public static void main(String[] args) throws IOException {
        while(true){
            System.out.println("Welcome to the TFTP client");
            System.out.println("Press 1 for read request");
            System.out.println("Press 2 for write request");
            int next = System.in.read();
            if (next == 49){
                System.out.println("Read request");
                new UDPSocketClient().readHandler("test.jpg", 9000, InetAddress.getLocalHost());
                continue;
            }
             if (next == 50){
                System.out.println("Write request");
                 new UDPSocketClient().writeHandler("tesfdfdt.txt", 9000, InetAddress.getLocalHost());
                 continue;
            }
            else{
                System.out.println("Invalid input");
            }
            System.out.println("File sent successfully!");
        }
    }
}
