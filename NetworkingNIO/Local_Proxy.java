package Networking_Project.NetworkingNIO;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

class Local_Proxy {
    private static Socket server;

    public static void main(String[] args) {
        encrypter en;
        decrypter de;

        if(args.length != 2){
            System.out.println("Usage : <Server's IP Address> <Server's Port>");
            return;
        }

        try {
            server = new Socket(args[0],Integer.parseInt(args[1]));
            de = new decrypter(server.getInputStream());
            en = new encrypter(server.getOutputStream());
        } catch (IOException e) {
            System.out.println("Could not find the server at the address/port.");
            return;
        } catch (NumberFormatException n){
            System.out.println("The port should be a valid number!");
            return;
        }

        outer : while (true) {
            try (Scanner in = new Scanner(System.in)) {
                while (true) {
                    String input = in.nextLine();
                    if(input.equalsIgnoreCase("<<<Logout>>>")) {
                        en.encrypter_thread.interrupt();
                        break outer;
                    }
                    en.messages_queue.put(input);
                }
            }catch(InterruptedException ignored) { }
        }

    }
}

class encrypter implements Runnable{
    static BlockingQueue<String> messages_queue = new ArrayBlockingQueue<>(10);
    private CharsetEncoder CharacterSet_encoder = StandardCharsets.US_ASCII.newEncoder();
    private CharBuffer charBuffer;
    private ByteBuffer byteBuffer = ByteBuffer.allocate(5120);
    private DataOutputStream writer;
    Thread encrypter_thread;
    static long seed;
    int message_sent = 0;

    encrypter(OutputStream out){
        writer = new DataOutputStream(out);
        encrypter_thread = new Thread(this);
        encrypter_thread.start();
    }

    private void encrypt(){
        Random random = new Random(seed);
        byte modified;
        // Skipping
        while(byteBuffer.hasRemaining()){
            modified = byteBuffer.get();
            modified = (byte) (((modified & 0xFF) << (message_sent % 8)) | ((modified & 0xFF) >> (8 - (message_sent % 8))));
            byteBuffer.put(byteBuffer.position() - 1, modified);
        }
        byteBuffer.flip();
        // Substitution
        while(byteBuffer.hasRemaining()){
            modified = byteBuffer.get();
            modified = (byte) ((modified & 0xFF) - random.nextInt(256)); // this will be done as many bytes are there
            byteBuffer.put(byteBuffer.position() - 1, modified);
        }
        byteBuffer.flip();
        // XORing
        while(byteBuffer.hasRemaining()){
            modified = byteBuffer.get();
            modified = (byte) ((modified & 0xFF) ^ random.nextInt());
            byteBuffer.put(byteBuffer.position() - 1, modified);
        }
        byteBuffer.flip();
        message_sent++;
    }

    @Override
    public void run() {
        String input;
        for( ; ; ){
            try {
                input = messages_queue.take();
                charBuffer = CharBuffer.wrap(input);
                CharacterSet_encoder.encode(charBuffer,byteBuffer,true);
                byteBuffer.flip();
                encrypt();
                writer.writeInt(byteBuffer.limit());
                writer.write(byteBuffer.array(),0,byteBuffer.limit());
                byteBuffer.clear();

                seed++;
            } catch (IOException i) {
                System.out.println("Message could not be sent, try again!");
                byteBuffer.clear();
            } catch (InterruptedException e) {
                System.out.println("Logged out successfully!!!");
                break;
            }
        }
    }
}

class decrypter implements Runnable{
    private CharsetDecoder CharacterSet_decoder = StandardCharsets.US_ASCII.newDecoder();
    private ByteBuffer byteBuffer = ByteBuffer.allocate(5120);
    Thread decrypter_thread;
    private InputStream in;
    private long seed;

    decrypter(InputStream in){
        this.in = in;
        decrypter_thread = new Thread(this);
        decrypter_thread.start();
    }

    private void decrypt(int offset){
        Random random = new Random(seed);
        int [] bounded_int = new int[offset];
        for (int i = 0; i < offset; i++) bounded_int[i] = random.nextInt(256);
        byte modified;
        // XORing
        while(byteBuffer.hasRemaining()){
            modified = byteBuffer.get();
            modified = (byte) ((modified & 0xFF) ^ random.nextInt());
            byteBuffer.put(byteBuffer.position() - 1, modified);
        }
        byteBuffer.reset();
        // Substution
        int i = 0;
        while(byteBuffer.hasRemaining()){
            modified = byteBuffer.get();
            modified = (byte) ((modified & 0xFF) + bounded_int[i]);
            i++;
        }
        byteBuffer.reset();
        // No Skipping as the server will only perform the above two when sending to other clients! (Planned)
    }

    @Override
    public void run() {
        int num = 0;
        CharBuffer charBuffer = CharBuffer.allocate(2500);
        final OutputStreamWriter out = new OutputStreamWriter(System.out,StandardCharsets.UTF_8);

        while (num != 16){
            try {
                int bytes_read = in.read(byteBuffer.array());
                num += bytes_read;
            } catch (IOException e) {
                System.out.println("Could not configure the encryption key!");
                num = 0;
                // Need to code for the request to server to send a different key again!
            }
        }
        num = 0;

        byteBuffer.limit(16);
        seed = byteBuffer.getLong();
        encrypter.seed = byteBuffer.getLong();
        byteBuffer.clear();

        int length = 0;
        while (true) {
            try {
                length += num;
                if ((num = in.read(byteBuffer.array(),length, byteBuffer.capacity())) < 4 && length < 4) continue;
                else length += num;
                byteBuffer.position(length);
                length = byteBuffer.getInt(0);
                byteBuffer.limit(length + 4);
                while(byteBuffer.hasRemaining()){
                    num = in.read(byteBuffer.array(),byteBuffer.position(),byteBuffer.limit());
                    byteBuffer.position(byteBuffer.position() + num);
                }
                byteBuffer.position(4);
                byteBuffer.mark();
                decrypt(length);
                CharacterSet_decoder.decode(byteBuffer,charBuffer,true);
                charBuffer.flip();
                out.write(charBuffer.array());
                out.flush();

                // Don't know if I need to do this or not (Not energetic enough to not trace back the flow to check!)
                byteBuffer.clear();
                charBuffer.clear();
                seed++;

            } catch (IOException e) {
                System.out.println("Could not read message, ask others to try again!");
            }
        }
    }
}