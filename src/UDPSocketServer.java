import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.Arrays;
import java.util.Date;

public class UDPSocketServer extends Thread {
    byte[] recvBuf = new byte[256];
    DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);
    protected DatagramSocket socket = null;
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
        System.out.println(Arrays.toString(errResponsePacket));
        socket.send(packet);
    }
    public UDPSocketServer() throws SocketException, IOException {
        this("UDPSocketServer");
    }

    public UDPSocketServer(String name) throws IOException {
        super(name);
        InetAddress inetAddress = InetAddress.getLocalHost();
        System.out.println("Local IP address: " + inetAddress.getHostAddress());
        socket = new DatagramSocket(9000, inetAddress);
    }

    @Override
    public void run() {


        try {
            while (true) {
                String mode = "";
                String filename = "";
                DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);
                try {
                    System.out.println("Waiting for client ...");
                    socket.receive(packet);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                recvBuf = packet.getData();
                if ((recvBuf[0] != 0 && recvBuf[1] != 1) || (recvBuf[0] != 0 && recvBuf[1] != 2)){
                    System.out.println("Erroneous packet received ... ");
                    sendError(1, "Erroneous packet received", packet.getAddress(), packet.getPort());
                    continue;
                }
                System.out.println("Processing read request ... ");
                int currentByte = 2;
                while(recvBuf[currentByte] != 0){
                    filename += (char) recvBuf[currentByte];
                    currentByte++;
                }
                currentByte++;
                while(recvBuf[currentByte] != 0){
                    mode += (char) recvBuf[currentByte];
                    currentByte++;
                }
                if (!mode.equals("octet")){
                    System.out.println("Mode: " + mode + " not supported!" );
                    sendError(0, "Illegal TFTP operation", packet.getAddress(), packet.getPort());
                    continue;
                }
                try {
                    System.out.println("Thread id: "+threadId());
                    synchronized(this) {
                        wait(10000);
                    }

                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                Boolean isRead = (recvBuf[1] == 1);
                //ServerRequestHandler handler = new ServerRequestHandler(socket, packet, filename, isRead);
                //Thread thread = new Thread(handler);
                //thread.start();

                try {
                    socket.send(packet);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            socket.close();
        }
    }

    public static void main(String[] args) throws IOException {
        new UDPSocketServer().start();
        System.out.println("Time Server Started");
    }

}
