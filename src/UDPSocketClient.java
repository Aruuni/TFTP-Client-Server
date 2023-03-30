import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;
import java.util.Random;
import java.util.Scanner;

public class UDPSocketClient {
    protected DatagramSocket socket;
    protected DatagramPacket packet = new DatagramPacket(new byte[516], 516);
    protected DatagramPacket ackPacket = new DatagramPacket(new byte[4], 4);

    /**
     * Constructor for the client, only creates the socket on a random port from 1024 to 65533
     * @throws SocketException
     */
    public UDPSocketClient() throws SocketException {
        Random random = new Random();
        int randomNumber = random.nextInt((65533 - 1024) + 1) + 1024;
        socket = new DatagramSocket(randomNumber);
    }
    /**
     * Handles the read request
     * @param filename the name of the file to read
     * @param PORT the port to send the request to
     * @param address the address to send the request to
     * @throws IOException
     */
    public void readHandler(String filename, int PORT, InetAddress address) throws IOException {
        System.out.println("Reading file: " + filename);
        this.request((byte)1, filename, PORT, address);
        int blockNumber = 0;
        try {
            while (true) {
                //waiting for packet
                try {
                    socket.receive(packet);
                    socket.getSoTimeout();
                } catch (IOException e) {
                    continue;
                }
                byte[] rcvBuf = packet.getData();
                //checking for file not found error mainly
                if (rcvBuf[1] == 5) {
                    System.out.println("Error code: " + rcvBuf[3] + " Error message: " + new String(rcvBuf, 4, packet.getLength() - 5));
                    break;
                }
                FileOutputStream fos = new FileOutputStream(Paths.get("./ClientFiles/",filename).toFile());
                //ack the data packet and if it macthes the block number write it to the file
                sendAck(((rcvBuf[3]&0xff)|((rcvBuf[2]&0xff) << 8)), packet);
                if (((rcvBuf[3] & 0xff)|((rcvBuf[2]&0xff) << 8)) == blockNumber + 1) {
                    fos.write(rcvBuf, 4, packet.getLength() - 4);
                    blockNumber++;
                }
                //if the packet is less than 516 bytes then it is the last packet
                if (packet.getLength() < 516) {
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            socket.close();
        }
    }
    /**
     * Handles the write request
     * @param filename the name of the file to write
     * @param PORT the port to send the request to
     * @param address the address to send the request to
     * @throws IOException
     */
    public void writeHandler(String filename, int PORT, InetAddress address) throws IOException {
        System.out.println("Writing file: " + filename);
        try {
            //read the file into a byte array that I then read block my block, into a ClientFiles folder which makes it easier to see the files
            byte[] fileData = Files.readAllBytes(Paths.get("./ClientFiles/",filename));
            //send the write request
            this.request((byte)2, filename, PORT, address);
            int blockNumber = 1;
            try {
                //calculate the number of packets that will be sent
                int numPackets = (int) Math.ceil((double) fileData.length / 512);
                //edge case in which a file is a multiple of 512 bytes, in which case an extra packet is sent to indicate the end of the transfer
                if (fileData.length % 512 == 0 && fileData.length != 0) {
                    numPackets++;
                }
                //send the first packet block by block, if the block number is not incremented then the same data is sent
                while (blockNumber < numPackets) {
                    //wit for the ack packet
                    try {
                        socket.receive(ackPacket);
                    } catch (IOException e) {
                        continue;
                    }
                    //set the address and port of the packet to the address and port of the ack packet, this is necessary as the first ack packet is sent to a different port
                    packet.setAddress(ackPacket.getAddress());
                    packet.setPort(ackPacket.getPort());
                    //if the ack packet matches the block number, then send the next packet, if not the same packet is resent as it has not been acked
                    if (((ackPacket.getData()[3]&0xff)|((ackPacket.getData()[2]&0xff) << 8)) == blockNumber){
                        blockNumber++;
                    }
                    //send the correct block from the fileData using start index and end index, the end index is the start index + 512 unless it is the last block, so I use the min function
                    int startIndex = (blockNumber-1) * 512;
                    int endIndex = Math.min(startIndex + 512, fileData.length);
                    //create the data packet and send it after the correct block of data was written to it
                    byte[] data = new byte[endIndex - startIndex + 4];
                    data[0] = 0;
                    data[1] = 3;
                    //shifting an int by 8 bits
                    data[2] = (byte) ((blockNumber>>8)&0xff);
                    data[3] = (byte) ((blockNumber)&0xff);
                    //copies the data into the data packet
                    System.arraycopy(Arrays.copyOfRange(fileData, startIndex, endIndex), 0, data, 4, endIndex - startIndex);
                    DatagramPacket dataPacket = new DatagramPacket(data, data.length, packet.getAddress(), packet.getPort());
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
    /**
     * Sends a request packet to the server
     * @param opcode the opcode of the request (1 for read, 2 for write)
     * @param filename the name of the file to request
     * @param PORT the port to send the request to
     * @param address the address to send the request to
     * @throws IOException
     */
    public void request(byte opcode,String filename, int PORT, InetAddress address) throws IOException {
        byte[] Opcode = new byte[2];
        Opcode[1] = opcode;
        byte[] fileName = filename.getBytes();
        byte[] Mode = "octet".getBytes();
        byte[] request = new byte[265];
        //copy the opcode, filename and mode into the request byte array
        System.arraycopy(Opcode, 0, request, 0, Opcode.length);
        System.arraycopy(fileName, 0, request, Opcode.length, fileName.length);
        System.arraycopy(Mode, 0, request, Opcode.length + fileName.length + 1, Mode.length);
        //send the request packet to the server
        DatagramPacket requestPacket = new DatagramPacket(request, 265, address, PORT);
        socket.send(requestPacket);
    }
    /**
     * Sends an ack packet to the client
     * @param block the block number to send
     * @param toAck the packet object used to get the destination of the ack
     * @throws IOException
     */
    public void sendAck(int block, DatagramPacket toAck) throws IOException {
        //build the ack packet
        byte[] ack = new byte[4];
        ack[1] = 4;
        //block number is 2 bytes so a bit shift is needed
        ack[2] = (byte) ((block>>8) & 0xff);
        ack[3] = (byte) (block&0xff);
        DatagramPacket ackPacket = new DatagramPacket(ack, 4, toAck.getAddress(), toAck.getPort());
        socket.send(ackPacket);
    }
    /**
     * Handles the read request
     * @param args for the IP address
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        while(true){
            System.out.println("Welcome to the TFTP client");
            System.out.println("Press 1 for read request");
            System.out.println("Press 2 for write request");
            Scanner scanner = new Scanner(System.in);
            String next = scanner.nextLine();
            if (Objects.equals(next, "1")){
                System.out.println("Enter the file name");
                String filename = scanner.nextLine();
                new UDPSocketClient().readHandler(filename, 69, InetAddress.getLocalHost());
                System.exit(0);
            }
            if (Objects.equals(next, "2")){
                System.out.println("Enter the file name");
                String filename = scanner.nextLine();
                new UDPSocketClient().writeHandler(filename, 69, InetAddress.getLocalHost());
                System.exit(0);
            }
            else{
                System.out.println("Invalid input");
            }
        }
    }
}
