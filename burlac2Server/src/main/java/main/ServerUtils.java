package main;

import encryption.AES;
import encryption.GeneratedKeys;
import encryption.RSAUtils;


import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;

import static encryption.GeneratedKeys.aesKey;
import static encryption.RSAUtil.encrypt;


public class ServerUtils {
    public static void processMessage(String str, DatagramSocket socket,  DatagramPacket packetToReceive,  String senderNickname) throws BadPaddingException, NoSuchAlgorithmException, IllegalBlockSizeException, NoSuchPaddingException, InvalidKeyException, IOException {
        boolean isCommand = str.startsWith("!!");
        int sw = isCommand == true ? 1 : 0;

        switch (sw) {
            case 1:
                processCommand(str, socket, packetToReceive);
                break;

            case 0:
                processText(str, socket, senderNickname);

                break;
            default:
                break;
        }
    }

    private static void processText(String str, DatagramSocket socket,  String senderNickname) throws IllegalBlockSizeException, NoSuchAlgorithmException, IOException, BadPaddingException, NoSuchPaddingException, InvalidKeyException {
        for (User u : UdpMulticastServer.userList) {

            if (u.getIpPort() != 0) {
                if(u.isLogged()) {
                    String strToSend = senderNickname + ": " + str;
                    sendMessage(socket,  strToSend, u.getIpPort());
                }
            }
        }
    }

    public static void processCommand(String str, DatagramSocket socket, DatagramPacket packetToReceive) throws BadPaddingException, NoSuchAlgorithmException, IllegalBlockSizeException, NoSuchPaddingException, InvalidKeyException, IOException {
        //str = str.substring(1);
        int port = packetToReceive.getPort();
        boolean incorrectCommand = true;
        String[] arr = str.split(" ");
        if (arr.length > 1) {
            switch (arr[1]) {

                case "resend":
//                    sendMessage(socket,  UdpMulticastServer.prevMessage, port);
                    processText(UdpMulticastServer.prevMessage, socket, "resent.: ");
                break;

                case "login":
                    String username = arr[2];
                    String password = arr[3];
                    login(socket, username, password, port);
                    incorrectCommand = false;
//!! login user pas
                    break;
                case "change":
//!! change nickname newMickname
                    if (arr[2].equals("nickname")) {
                        for (User u : UdpMulticastServer.userList) {
                            if (u.isLogged()) {
                                if (u.getIpPort() == port) {
                                    incorrectCommand = false;
                                    u.setNickname(arr[3]);
                                    sendMessage(socket,  "SYSTEM: nickname changed.", port);
                                    break;
                                }
                            }
                        }
                    }
                    break;
                case "help":
                    incorrectCommand = false;
                    sendMessage(socket,  "SYSTEM: '!! login username password'\n        '!! change nickname NewNickname'\n You already figured out about '!! help' somehow", port);
                    break;
                default:

                    break;
            }
        }
        if (incorrectCommand == true) {
            sendMessage(socket,  "SYSTEM: Command is not recognized. All commands start with '!!', for the list of all commands type '!! help'", port);
        }

    }

    public static void login(DatagramSocket socket, String username, String password, int ipPort) throws IllegalBlockSizeException, NoSuchAlgorithmException, IOException, BadPaddingException, NoSuchPaddingException, InvalidKeyException {
        boolean exists = false;
        for (User u : UdpMulticastServer.userList) {
            if (u.getUsername().equals(username) && u.getPassword().equals(password)) {
                exists = true;
                if (!u.isLogged()) {
                    u.setIpPort(ipPort);
                    u.setLogged(true);
                    sendMessage(socket,  "SYSTEM: " + u.getUsername() + " succesfully logged in", ipPort);
                } else {
                    sendMessage(socket, "SYSTEM: " + u.getUsername() + "is already logged in", ipPort);
                }
            }
        }

        if (exists == false) {
            sendMessage(socket, "SYSTEM: incorrect credentials", ipPort);
        }


    }



    //this method ENCRYPTS text, and then sends it
    public static void sendMessage(DatagramSocket socket, String message, int clientPort) throws IOException, IllegalBlockSizeException, NoSuchPaddingException, BadPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        message = textToCipher(message);


        message = getPacketNumber(message.length()) + "!!!" + message;
        int msgLength = message.length();

        if (msgLength > 1024) {
            int numberOfPackets = getPacketNumber(msgLength);
            int startInd = 0;
            int endInd = 0;
            String onePacketMessage;
            for (int i = 0; i < numberOfPackets; i++) {
                startInd = i * 1024;
                if (i != numberOfPackets - 1) {
                    endInd = startInd + 1024;
                } else {
                    endInd = msgLength;
                }

                onePacketMessage = message.substring(startInd, endInd);
                byte[] arb = new byte[1024];



                DatagramPacket packet = new DatagramPacket(arb, arb.length, InetAddress.getLocalHost(), clientPort);
                packet.setData(onePacketMessage.getBytes());
                socket.send(packet);
                Arrays.fill(arb, (byte) 0);
            }
        } else {

            byte[] arb = new byte[1024];

            DatagramPacket packet = new DatagramPacket(arb, arb.length, InetAddress.getLocalHost(), clientPort);
            packet.setData(message.getBytes());
            socket.send(packet);
            Arrays.fill(arb, (byte) 0);
        }


    }

    public static long getHashSum(String str) {
        int strlen = str.length();

        long hash = 7;
        for (int i = 0; i < strlen; i++) {
            hash = hash * 31 + str.charAt(i);
        }
        return hash;
    }

    public static int getPacketNumber(int msgLength) {
        int rough = msgLength / 1024;
//        System.out.println("rough: "+rough);
        int mod = msgLength % 1024;
//        System.out.println("mod: "+mod);
        if (mod != 0) {
            rough++;
        }
//        System.out.println("precise: "+rough);
        return rough;
    }

    public static String textToCipher(String text) throws NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, NoSuchPaddingException, IllegalBlockSizeException {
        //1 get a string
        String msg = text;
        //2 get its hash
        long hash = getHashSum(msg);
        //3 get AES KEY. don't worry, he is set locally in GeneratedKeys

        //3.5 add hash
        msg += "~~~" + hash;
        //4 encrypt msg with aes
        String aesEncryptedMsg = AES.encrypt(msg, aesKey);
        //5 encrypt aesKey with RSA
        String rsaEncryptedAesKey = encryptString(aesKey);
        //6  combine msg with encrypted aesKey using ###
        aesEncryptedMsg += "###" + rsaEncryptedAesKey;
        return aesEncryptedMsg;
    }

    public static String magicWithCipher(String str) {
        String[] arr = str.split("###");
        String newSecond = arr[1].split("=")[0];
        return arr[0] + "###" + newSecond + "=";
    }

    public static String cipherToText(String cipher) throws InvalidKeyException, BadPaddingException, NoSuchAlgorithmException, IllegalBlockSizeException, NoSuchPaddingException {
        String aesEncryptedMsg = cipher;
        //7 split with ### , second part decrypt using RSA private key and get aes key
        String[] incomeArr = aesEncryptedMsg.split("###");
//        //7.5 get aes key
        String decryptedAesKey = RSAUtils.decrypt(incomeArr[1], GeneratedKeys.privateKey);
        //8 decrypt with aes key
        String decryptedMessage = AES.decrypt(incomeArr[0], decryptedAesKey);
        //9 check hashes (msg~~~hash)
        String[] arr = decryptedMessage.split("~~~");
        if (getHashSum(arr[0]) == Long.valueOf(arr[1])) {
            return arr[0];
        } else {
            return "!! resend"; //"ERROR. Hash check failed.";
        }
        //10 profit?
    }

    public static String encryptString(String msg) throws IllegalBlockSizeException, InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, BadPaddingException {
        return Base64.getEncoder().encodeToString(encrypt(msg, GeneratedKeys.publicKey));
    }
}

//! login username password
//! change nickname newNickname
//! shutdownServer


