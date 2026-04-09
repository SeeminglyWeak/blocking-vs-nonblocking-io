package Networking_Project.NetworkingNIO;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

class Encrypter implements Runnable{
    static BlockingQueue<String> messages_queue = new ArrayBlockingQueue<>(10);
    private CharsetEncoder CharacterSet_encoder = StandardCharsets.US_ASCII.newEncoder();
    private CharBuffer charBuffer;
    private ByteBuffer byteBuffer = ByteBuffer.allocate(5120);
    private DataOutputStream writer;
    Thread Encrypter_thread;
    static long seed;
    private int message_sent = 0;

    Encrypter(OutputStream out){
        writer = new DataOutputStream(out);
        Encrypter_thread = new Thread(this);
    }

    private void encrypt(){
        Random random = new Random(seed);
        byte modified;
        while(byteBuffer.hasRemaining()){
            // Skipping
            modified = byteBuffer.get();
            modified = (byte) (((modified & 0xFF) << (message_sent % 8)) | ((modified & 0xFF) >> (8 - (message_sent % 8))));
            // Substitution
            modified = (byte) ((modified & 0xFF) - random.nextInt(256)); 
            // XORing
            modified = (byte) ((modified & 0xFF) ^ random.nextInt());
            byteBuffer.put(byteBuffer.position() - 1, modified);
        }
        byteBuffer.rewind();
        message_sent++;
    }

    @Override
    public void run() {
        String input;
        for( ; ; ){
            try {
                input = messages_queue.take(); // Interruptible 
                charBuffer = CharBuffer.wrap(input);
                CharacterSet_encoder.encode(charBuffer,byteBuffer,true);
                byteBuffer.flip();
                encrypt();
                writer.writeInt(byteBuffer.limit());
                writer.write(byteBuffer.array(),0,byteBuffer.limit());
                writer.flush();
                byteBuffer.clear();

                seed++;
            } catch (IOException i) {
                System.out.println("Message could not be sent, Connection reset by server!");
                try{
                    System.in.close();
                } catch (IOException ignored) {}
                break;
            } catch (InterruptedException e) {
                System.out.println("Closed writing to the socket!");
                break;
            }
        }
    }
}