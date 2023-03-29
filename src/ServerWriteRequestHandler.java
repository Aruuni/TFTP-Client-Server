import java.io.FileOutputStream;
import java.io.IOException;
import java.net.*;
import java.util.Arrays;

public class ServerWriteRequestHandler extends Thread {

    private DatagramSocket socket;
    private DatagramPacket packet = new DatagramPacket(new byte[516], 516);
    private int blockNumber = 0;
    private String filename;
    public ServerWriteRequestHandler(int PORT, DatagramPacket packet, String filename) throws SocketException, UnknownHostException {
        this.packet.setPort(packet.getPort());
        this.packet.setAddress(packet.getAddress());
        this.filename = filename;
        this.socket = new DatagramSocket(PORT, InetAddress.getLocalHost());
        socket.setSoTimeout(1000);
    }
    public void sendAck(int block, DatagramPacket toAck) throws IOException {
        byte[] ack = new byte[4];
        ack[1] = 4;
        ack[2] = (byte) ((block>>8) & 0xff);
        ack[3] = (byte) (block&0xff);
        DatagramPacket ackPacket = new DatagramPacket(ack, 4, toAck.getAddress(), toAck.getPort());
        socket.send(ackPacket);
    }

    public void run() {
        System.out.println("Processing write request ..." + filename);
        int blockNumber = 0;
        try {
            sendAck(0, packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try (FileOutputStream fos = new FileOutputStream(filename)) {
            while (true) {
                try {
                    System.out.println("waiting for data");
                    socket.receive(packet);
                    socket.getSoTimeout();
                } catch (IOException e) {
                    e.printStackTrace();
                    continue;
                }
                byte[] rcvBuf = packet.getData();
                System.out.println(Arrays.toString(rcvBuf));
                System.out.println(rcvBuf.length);
                fos.write(rcvBuf, 4, packet.getLength() - 4);
                sendAck(((rcvBuf[3]&0xff)|((rcvBuf[2]&0xff) << 8)), packet);
                if (((rcvBuf[3] & 0xff)|((rcvBuf[2]&0xff) << 8)) == blockNumber + 1) {
                    blockNumber++;
                }
                if (packet.getLength() < 516) {
                    System.out.println(packet.getLength());
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("File tarnsferrd");
        socket.close();
    }
}