import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

public class ServerReadRequestHandler extends Thread {

    protected DatagramSocket socket;
    protected DatagramPacket packet = new DatagramPacket(new byte[516], 516);
    protected DatagramPacket ackPacket = new DatagramPacket(new byte[4], 4);
    private int blockNumber = 1;
    private String filename;
    public ServerReadRequestHandler(int PORT, DatagramPacket packet, String filename) throws SocketException, UnknownHostException {
        this.packet.setPort(packet.getPort());
        this.packet.setAddress(packet.getAddress());
        this.filename = filename;
        this.socket = new DatagramSocket(PORT, InetAddress.getLocalHost());
        socket.setSoTimeout(1000);
    }
    public void sendError(int ErrCode, String ErrMsg, InetAddress address, int port) throws IOException {
        byte[] Opcode = new byte[2];
        Opcode[1] = 5;
        byte[] ErrorCode = new byte[2];
        ErrorCode[0] = (byte) (ErrCode >> 8);
        ErrorCode[1] = (byte) (ErrCode);
        byte[] ErrorMsg = ErrMsg.getBytes();
        byte[] errResponsePacket = new byte[Opcode.length + ErrorCode.length + ErrorMsg.length + 1];
        System.arraycopy(Opcode, 0, errResponsePacket, 0, Opcode.length);
        System.arraycopy(ErrorCode, 0, errResponsePacket, Opcode.length, ErrorCode.length);
        System.arraycopy(ErrorMsg, 0, errResponsePacket, Opcode.length + ErrorCode.length, ErrorMsg.length);
        packet = new DatagramPacket(errResponsePacket, errResponsePacket.length, address, port);
        socket.send(packet);
    }
    public void run() {
        System.out.println("Processing read request ..." + filename);
        try {
            byte[] fileData = Files.readAllBytes(Paths.get("./src/",filename));
            int packetSize = 512;
            int numPackets = (int) Math.ceil((double) fileData.length / packetSize);
            if(fileData.length % 512 == 0){
                numPackets++;
            }
            while(blockNumber <= numPackets){
                System.out.println("Block number: " + blockNumber);
                int startIndex = (blockNumber-1) * 512;
                int endIndex = Math.min(startIndex + 512, fileData.length);
                byte[] data = new byte[endIndex - startIndex + 4];
                data[0] = 0;
                data[1] = 3;
                data[2] = (byte) ((blockNumber>>8)&0xff);
                data[3] = (byte) ((blockNumber)&0xff);
                System.arraycopy(Arrays.copyOfRange(fileData, startIndex, endIndex), 0, data, 4, endIndex - startIndex);
                DatagramPacket dataPacket = new DatagramPacket(data, data.length, packet.getAddress(), packet.getPort());
                socket.send(dataPacket);
                try {
                    System.out.println("waiting for ACK");
                    socket.receive(ackPacket);
                } catch (IOException e) {
                    continue;
                }
                if (((ackPacket.getData()[3]&0xff)|((ackPacket.getData()[2]&0xff) << 8)) == blockNumber){
                    blockNumber++;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        socket.close();
        System.out.println("File sent successfully!");
    }
}