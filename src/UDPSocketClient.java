
import java.io.IOException;
import java.net.*;
import java.util.Arrays;

public class UDPSocketClient {
    private DatagramSocket socket;
    public DatagramPacket packet;
    private final int PORT = 4000;
    public UDPSocketClient() throws SocketException {
        socket = new DatagramSocket(PORT);
    }

    public void sendRRQ(String filename) throws UnknownHostException {
        byte[] Opcode = new byte[2];
        Opcode[1] = 1;
        byte[] fileName = filename.getBytes();
        byte[] Mode = "octett".getBytes();
        byte[] request = new byte[Opcode.length + fileName.length + Mode.length + 2];
        System.arraycopy(Opcode, 0, request, 0, Opcode.length);
        System.arraycopy(fileName, 0, request, Opcode.length, fileName.length);
        System.arraycopy(Mode, 0, request, Opcode.length + fileName.length + 1, Mode.length);
        System.out.println(Arrays.toString(request));
        packet = new DatagramPacket(request, request.length, InetAddress.getLocalHost(), 9000);
        System.out.println(request.length);
        packet.setData(request);
    }
    public void sendWRQ() throws IOException {
        byte[] Opcode = new byte[2];
        Opcode[1] = 2;
        byte[] Filename = "text.txt".getBytes();
        byte[] Mode = "octet".getBytes();
        byte[] request = new byte[Opcode.length + Filename.length + Mode.length + 2];
        System.arraycopy(Opcode, 0, request, 0, Opcode.length);
        System.arraycopy(Filename, 0, request, Opcode.length, Filename.length);
        System.arraycopy(Mode, 0, request, Opcode.length + Filename.length + 1, Mode.length);
        packet = new DatagramPacket(request, request.length, InetAddress.getLocalHost(), 9000);
        System.out.println(request.length);
        packet.setData(request);
    }
    public void sendAck(){

    }
    public void sendError(){

    }
    public void sendPacket(){

    }
    public void ackPacket(){

    }
    public void receivePacket() throws IOException {
        System.out.println("waiting for server ...");
        socket.receive(packet);
    }


    // the client will take the IP Address of the server (in dotted decimal format as an argument)
    // given that for this tutorial both the client and the server will run on the same machine, you can use the loopback address 127.0.0.1
    public static void main(String[] args) throws IOException {
        byte[] buf = new byte[512];
        UDPSocketClient client = new UDPSocketClient();
        if (args.length != 1) {
            System.out.println("the hostname of the server is required");
            return;
        }
        System.out.println("Welcome to the TFTP client");
        System.out.println("Press 1 for write request");
        System.out.println("Press 2 for read request");
        while(true){
            int next = System.in.read();
            next = next - 48;
            if (next == 1){
                System.out.println("Write request");

                client.sendRRQ("text.txt");
                client.socket.send(client.packet);
                try {
                    client.receivePacket();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            }
             if (next == 2){
                System.out.println("Read request");
                break;
            }
            else{
                System.out.println("Invalid input");
            }
            System.out.println();
        }
        buf = client.packet.getData();
        System.out.println(Arrays.toString(buf));
        if (buf[0] == 0 && buf[1] == 5){
            System.out.println("Error Code: " + (buf[2]<<8 + buf[3]) + " Error Message: " + new String(buf, 4, client.packet.getLength() - 4));
        }
        InetAddress address = InetAddress.getByName(args[0]);

        //packet = new DatagramPacket(buf, len);
        //packet.setAddress(address);

        //packet.setPort(9000);
        //packet.setData(buf);

        //socket.send(packet);


        //socket.receive(packet);

        // display response
        //String received = new String(packet.getData());
        //System.out.println("Today's date: " + received.substring(0, packet.getLength()));
        //socket.close();
    }

}
