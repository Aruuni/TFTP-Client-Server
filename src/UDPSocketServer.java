import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.Arrays;
import java.util.Date;

public class UDPSocketServer extends Thread {

    protected DatagramSocket socket = null;

    public UDPSocketServer() throws SocketException, IOException {
        this("UDPSocketServer");
    }

    public UDPSocketServer(String name) throws IOException {
        super(name);
        // **********************************************
        // TODO:
        // Add a line here to instantiate a DatagramSocket for the socket field defined above.
        // Bind the socket to port 9000 (any port over 1024 would be ok as long as no other application uses it).
        // Ports below 1024 require administrative rights when running the applications.
        // Take a note of the port as the client needs to send its datagram to an IP address and port to which this server socket is bound.
        //***********************************************
        //how do i get my current ip address and assign it to an inetaddress object
        InetAddress inetAddress = InetAddress.getLocalHost();
        System.out.println("Local IP address: " + inetAddress.getHostAddress());
        socket = new DatagramSocket(9000, inetAddress);




    }

    @Override
    public void run() {

        int counter = 0;                    // just a counter - used below
        byte[] recvBuf = new byte[256];     // a byte array that will store the data received by the client
        try {
            // run forever
            while (true) {
                //**************************************
                // TODO:
                // Add source code below to:
                // 1) create a DatagramPacket called packet. Use the byte array above to construct the datagram
                // 2) wait until a client sends something (a blocking call).
                //**************************************
                DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);
                //wait untill the client send soemthign
                try {
                    System.out.println("waiting for client to send something");
                    socket.receive(packet);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // Get the current date/time and copy it in the byte array
                String dString = new Date().toString() + " - Counter: " + (counter);
                int len = dString.length();                                             // length of the byte array
                byte[] buf = new byte[len];                                             // byte array that will store the data to be sent back to the client
                System.arraycopy(dString.getBytes(), 0, buf, 0, len);


                //****************************************
                // TODO:
                // Add source code below to extract the IP address (an InetAddress object) and source port (int) from the received packet
                // They will be both used to send back the response (which is now in the buf byte array -- see above)
                //****************************************
                InetAddress address = packet.getAddress();
                int port = packet.getPort();


                // set the buf as the data of the packet (let's re-use the same packet object)
                packet.setData(buf);

                //*****************************************
                // TODO:
                // set the IP address and port extracted above as destination IP address and port in the packet to be sent
                //*****************************************
                packet.setAddress(address);
                packet.setPort(port);
                String received = new String(buf);
                System.out.println("recived"+received);
                //*****************************************
                // TODO:
                // Add a line below to send the packet (a blocking call)
                //*****************************************
                try {
                    socket.send(packet);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // increase the counter
                counter++;
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
