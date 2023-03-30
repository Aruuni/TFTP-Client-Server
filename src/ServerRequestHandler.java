import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * This class handles the server side of the TFTP protocol
 * It is responsible for sending and receiving packets from the client on its own thread
 *
 * @author 249951
 * @version 1.0
 *
 */
public class ServerRequestHandler extends Thread {
    protected DatagramSocket socket;
    protected DatagramPacket packet = new DatagramPacket(new byte[516], 516);
    protected DatagramPacket ackPacket = new DatagramPacket(new byte[4], 4);
    private int blockNumber = 1;
    private final int mode;
    private final String filename;

    /**
     * Constructor for the ServerRequestHandler class
     *
     * @param PORT The port to listen on
     * @param packet The packet to be sent
     * @param filename The filename to be sent
     * @param mode The mode to be sent (1 for read, 2 for write)
     * @throws SocketException
     * @throws UnknownHostException
     */
    public ServerRequestHandler(int PORT, DatagramPacket packet, String filename, int mode) throws SocketException, UnknownHostException {
        this.packet.setPort(packet.getPort());
        this.packet.setAddress(packet.getAddress());
        this.filename = filename;
        this.mode = mode;
        this.socket = new DatagramSocket(PORT, InetAddress.getLocalHost());
        socket.setSoTimeout(1000);
    }
    /**
     * Sends an error packet to the client with a specific message an error code
     *
     * @param ErrCode the command line arguments
     * @param ErrMsg The error message to be sent to the client
     * @param address The address of the client
     * @param port The port of the client
     * @throws IOException
     */
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
    /**
     * Sends an ACK packet to the client
     *
     * @param block The block number to be sent
     * @param toAck The packet to be sent
     * @throws IOException
     */
    public void sendAck(int block, DatagramPacket toAck) throws IOException {
        byte[] ack = new byte[4];
        ack[1] = 4;
        ack[2] = (byte) ((block>>8) & 0xff);
        ack[3] = (byte) (block&0xff);
        DatagramPacket ackPacket = new DatagramPacket(ack, 4, toAck.getAddress(), toAck.getPort());
        socket.send(ackPacket);
    }
    /**
     * The read request handler for the server
     */
    public void readRequest() {
        try {
            // Check if file exists and closes the effectively closes the thread if it doesn't
            if(!new File("./ServerFiles/",filename).isFile()){
                System.out.println("File not found");
                sendError(1, "File not found", packet.getAddress(), packet.getPort());
                return;
            }
            //takes the file in a byte array. This byte array is then sent one block of 512 bytes at a time
            byte[] fileData = Files.readAllBytes(Paths.get("./ServerFiles/",filename));
            int packetSize = 512;
            int numPackets = (int) Math.ceil((double) fileData.length / packetSize);
            //edge case where the file is a multiple of 512 bytes to handle the edge case in which an empty packet need ot be sent to indicate the end of transmission
            if(fileData.length % 512 == 0){
                numPackets++;
            }
            //loop through all the blocks
            while(blockNumber <= numPackets){
                //I gather the data for the specific block based on the start index and end index. I then create a new byte array with the data and the opcode and block number
                int startIndex = (blockNumber-1) * 512;
                int endIndex = Math.min(startIndex + 512, fileData.length);
                byte[] data = new byte[endIndex - startIndex +4];
                data[0] = 0;
                data[1] = 3;
                //I use bit shifting to get the block number in the correct format
                data[2] = (byte) ((blockNumber>>8)&0xff);
                data[3] = (byte) ((blockNumber)&0xff);
                //I copy the data from the file into the data array at an offset of 4 due to the header
                System.arraycopy(Arrays.copyOfRange(fileData, startIndex, endIndex), 0, data, 4, endIndex - startIndex);
                //I create a new packet with the data and send it
                DatagramPacket dataPacket = new DatagramPacket(data, data.length, packet.getAddress(), packet.getPort());
                socket.send(dataPacket);
                //I wait for an ACK packet and check if the block number is correct. If it is, I increment the block number and continue. If it isn't, I resend the packet
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
        } finally {
            socket.close();
        }
        System.out.println("File sent successfully!");
    }
    /**
     * The write request handler for the server
     */
    public void writeRequest() {
        System.out.println("Processing write request ..." + filename);
        blockNumber = 0;
        //I send an ACK packet with a block number of 0 to indicate that the server is ready to receive data
        try {
            sendAck(0, packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //I create a new file output stream to write the data to the file
        try (FileOutputStream fos = new FileOutputStream("./ServerFiles/"+filename)) {
            while (true) {
                try {
                    //I wait for a data packet
                    socket.receive(packet);
                    socket.getSoTimeout();
                } catch (IOException e) {
                    continue;
                }
                //I check if the block number is correct. If it is, I write the data to the file and send an ACK packet. If it isn't, I send an ACK packet with the correct block number
                byte[] rcvBuf = packet.getData();
                System.out.println(Arrays.toString(rcvBuf));
                if (((rcvBuf[3] & 0xff)|((rcvBuf[2]&0xff) << 8)) == blockNumber+1) {
                    fos.write(rcvBuf, 4, packet.getLength() - 4);
                    blockNumber++;
                }
                sendAck(((rcvBuf[3]&0xff)|((rcvBuf[2]&0xff) << 8)), packet);
                //I check if the packet is less than 516 bytes. If it is, it is the last packet and I break out of the loop
                if (rcvBuf.length < 516) {
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            socket.close();
        }
        System.out.println("File received successfully!");
    }
    /**
     * The main method for the server, it either uses the read or write request handler, in my opinion it is better than havinh 2 separate classes
     */
    @Override
    public void run() {
        //I check if the packet is a read or write request and call the appropriate method
        if (mode == 1) {
            readRequest();
        } else {
            writeRequest();
        }
    }
}