package main;


import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.net.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UdpMulticastServer {
    public static List<User> userList = new ArrayList<>();
    public static String prevMessage;


    public static void main(String[] args) {
        userList.add(new User("client1", "1111"));
        userList.add(new User("client2", "1111"));
        userList.add(new User("client3", "1111"));


        int port = 20000;
        String entireMsg = "";
        int msgCount = 0;
        String[] msgArr = new String[2];


        try {
            DatagramSocket serverSocket = new DatagramSocket(port, InetAddress.getLocalHost());
            byte[] buffer = new byte[1024];  // max 8192 bytes

            DatagramPacket packetToReceive = new DatagramPacket(buffer, buffer.length);
            DatagramPacket packetToSend = new DatagramPacket(buffer, buffer.length);

            boolean whileLoop = true;


            while (whileLoop) {
//                serverSocket.receive(packetToReceive);

                serverSocket.receive(packetToReceive);
                String receivedMsg = new String(packetToReceive.getData());
                String senderNickname = "";

                for (User u : userList) {
                    if (u.getIpPort() == packetToReceive.getPort()) {
                        senderNickname = u.getNickname();
                    }
                }

                msgArr = receivedMsg.split("!!!");
                msgCount = Integer.valueOf(msgArr[0]);
                entireMsg += (msgArr[1]);

                if (msgCount > 1) {
                    for (int i = 1; i < msgCount; i++) {
                        serverSocket.receive(packetToReceive);
                        entireMsg += (new String(packetToReceive.getData()));
                    }
                }
                String decryptedResult = ServerUtils.cipherToText(ServerUtils.magicWithCipher(entireMsg.trim()));
                prevMessage = decryptedResult;
//                System.err.println("DECRYPTED RESULT: "+decryptedResult);
                ServerUtils.processMessage(decryptedResult, serverSocket, packetToReceive, senderNickname);

                System.out.printf("Data from client : %s:%d, and text : %s\n", packetToReceive.getAddress().getHostAddress(), packetToReceive.getPort(), entireMsg); // printf not println
                System.out.print("///// " + decryptedResult);
                decryptedResult = new String("");
                entireMsg = new String("");
                receivedMsg = new String("");
//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

                System.out.println();
                for (User u : userList) {
                    System.out.print(" [" + u.getIpPort() + " ]");
                }


                Arrays.fill(buffer, (byte) 0);


            }


        } catch (SocketException | UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        }
    }


}
