package Networking_Project.NetworkingNIO;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Random;

class Decrypter implements Runnable{
    private CharsetDecoder CharacterSet_decoder = StandardCharsets.US_ASCII.newDecoder();
    private ByteBuffer byteBuffer = ByteBuffer.allocate(5120);
    Thread Decrypter_thread;
    private InputStream in;
    private long seed;
    private int message_received = 0;

    Decrypter(InputStream in){
        this.in = in;
        Decrypter_thread = new Thread(this);
    }

    void kill_process(String reason){
        System.out.println(reason);
        LocalProxy.en.Encrypter_thread.interrupt();
        try{
            System.in.close();
        } catch (IOException ignored) { }
        Thread.currentThread().interrupt();
    }

    private void decrypt () {
        Random random = new Random(seed);
        byte modified;
        // XORing
        while(byteBuffer.hasRemaining()){
            modified = byteBuffer.get();
            int bounded_int = random.nextInt(256);
            modified = (byte) ((modified & 0xFF) ^ random.nextInt());
            // Substution  
            modified = (byte) ((modified & 0xFF) + bounded_int);
            // Skipping 
            modified = (byte) (((modified & 0xFF) << (8 - (message_received % 8))) | ((modified & 0xFF) >> (message_received % 8)));
            byteBuffer.put(byteBuffer.position() - 1, modified);  
        }
        byteBuffer.reset();
    }

    @Override
    public void run() {
        int num = 0;
        CharBuffer charBuffer = CharBuffer.allocate(2500);
        final OutputStreamWriter out = new OutputStreamWriter(System.out,StandardCharsets.UTF_8);

        while (num != 16){
            try {
                int bytes_read = in.read(byteBuffer.array(), num, 16 - num);
                if(bytes_read == -1) {
                    kill_process("Could not configure the encryption key!\nConnection reset by server, try again!");
                    return;
                }
                num += bytes_read;
            } catch (IOException e) {
                System.out.println("Could not configure the encryption key!\nConnection reset by server, try again!");
                LocalProxy.en.Encrypter_thread.interrupt();
                try{
                    System.in.close();
                } catch (IOException ignored) {}
                return;
            }
        }
        num = 0;

        byteBuffer.limit(16);
        seed = byteBuffer.getLong();
        Encrypter.seed = byteBuffer.getLong();
        byteBuffer.clear();

        int length = 0, bytes_read = 0;
        while (true) { 

            try {
                length += num;
                if ((num = in.read(byteBuffer.array(),length, 4 - length)) < 4 && length < 4){ 
                    if(num == -1) {
                        kill_process("Connection reset by server!");
                        break;
                    }
                    continue;
                } else length += num;
                byteBuffer.position(4);
                length = byteBuffer.getInt(0);
                byteBuffer.limit(length + 4);
                num = 0;
                while(byteBuffer.hasRemaining()){
                    num = in.read(byteBuffer.array(),byteBuffer.position(),length - bytes_read);
                    if(num == -1) {
                        kill_process("Connection reset by server!");
                        return;
                    }
                    byteBuffer.position(byteBuffer.position() + num);
                    bytes_read += num; 
                }
                bytes_read = num = 0;
                byteBuffer.position(4);
                byteBuffer.mark();
                decrypt();
                CharacterSet_decoder.decode(byteBuffer,charBuffer,true);
                charBuffer.flip();
                out.write(charBuffer.array());
                out.flush();

                byteBuffer.clear();
                charBuffer.clear();
                seed++;

            } catch (SocketException s) {
                System.out.println("Closed listening on the socket!");
                break;
            } catch (IOException e) {
                System.out.println("Connection reset by server!");
                LocalProxy.en.Encrypter_thread.interrupt();
                try{
                    System.in.close();
                } catch (IOException ignored) { }
                break;
            }

        }
    }
}