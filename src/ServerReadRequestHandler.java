import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.*;
import java.util.Arrays;

public class ServerReadRequestHandler extends Thread {

    protected DatagramSocket socket;
    protected DatagramPacket packet;
    private int blockNumber = 0;
    private String filename;
    public ServerReadRequestHandler(DatagramSocket socket, DatagramPacket packet, String filename) {
        this.socket = socket;
        this.packet = packet;
        this.filename = filename;
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

        try {
            File file = new File(filename);
            byte[] fileData = new byte[(int) file.length()];
            try (FileInputStream fileInputStream = new FileInputStream(file)) {
                fileInputStream.read(fileData);
            } catch (IOException e) {
                e.printStackTrace();
            }
            int packetSize = 512;
            int numPackets = (int) Math.ceil((double) fileData.length / packetSize);
            byte[][] packets = new byte[numPackets][];
            for (int i = 0; i < numPackets; i++) {
                int startIndex = i * packetSize;
                int endIndex = Math.max(startIndex + packetSize, fileData.length);
                packets[i] = Arrays.copyOfRange(fileData, startIndex, endIndex);
            }
            while(blockNumber <= numPackets){
                byte[] data = new byte[516];
                data[0] = 0;
                data[1] = 3;
                data[2] = (byte) (blockNumber >> 8);
                data[3] = (byte) (blockNumber);
                System.arraycopy(packets[blockNumber], 0, data, 4, packets[blockNumber].length);
                DatagramPacket dataPacket = new DatagramPacket(data, data.length, packet.getAddress(), packet.getPort());
                socket.send(dataPacket);
                try {
                    System.out.println("Waiting for ack ...");
                    socket.receive(packet);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                byte[] recvBuf = packet.getData();
                if (recvBuf[0] != 0 || (recvBuf[1] != 3)){
                    sendError(4, "Illegal TFTP operation", packet.getAddress(), packet.getPort());
                }
                else {
                    if ( (recvBuf[2]<<8 + recvBuf[3]) == blockNumber){
                        blockNumber++;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}