/*
* This class serves the role of having a single thread which will keep reading the data from the other threads.
* After reading the data in the thread, it will use a buffer to pass it on to every other client connected.
*/

package NetworkingNIO;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

class wrapper{
    String msg;
    SocketChannel sender;

    wrapper(String msg, SocketChannel sender){
        this.msg = msg;
        this.sender = sender;
    }

}

// Already configured when it is gonna be initialized
class server_Read implements Runnable{
    Thread t;
    static BlockingQueue<SelectionKey> keys = new LinkedBlockingQueue<>();

    server_Read(){
        t = new Thread(this);
        t.start();
    }

    @Override
    public void run() {
        while(true){
            try {
                SelectionKey key = keys.take();
                SocketChannel client = (SocketChannel) key.channel();
                ByteBuffer buffer = (ByteBuffer) key.attachment();

                int bytes;
                String data = "";
                while((bytes = client.read(buffer)) > 0){ // Gaurds against the client disconnected or no data is left in the buffer
                    buffer.flip();
                    data += StandardCharsets.UTF_8.decode(buffer);
                    buffer.clear();
                }
                System.out.println("Data received -> " + data); // Debug statement!
                if(bytes == -1){
                    System.out.println("client disconnected from -> " + client.getRemoteAddress());
                    client.close();
                    key.cancel();
                    continue;
                }

                server_Write.data.put(new wrapper(data,client));
            } catch (InterruptedException e) {
                // Will decide the fail safe after!
                e.printStackTrace();
            } catch (IOException i) {
                // Will decide the fail safe after!
                i.printStackTrace();
            }
        }
    }

}

//Already initalized the writer thread starting point!
class server_Write implements Runnable{
    Thread t;
    ByteBuffer write = ByteBuffer.allocateDirect(1024 * 512);
    static BlockingQueue<wrapper> data = new LinkedBlockingQueue<>();
    Selector selector;

    server_Write(Selector selector){
        this.selector = selector;
        t = new Thread(this);
        t.start();
    }

    @Override
    public void run(){
        while(true){
            wrapper sender = null;
            try {
                sender = data.take();
                System.out.println("Data received by the write thread in queue -> " + sender.msg);
                byte[] arr = sender.msg.getBytes(StandardCharsets.UTF_8); // Still need to think about the size problem
                write.put(arr); // Wrap does not work here as it returns a new bytebuffer
            } catch (InterruptedException e) {
                e.printStackTrace();

            }
            Iterator<SelectionKey> itr = selector.keys().iterator();
            SocketChannel client;
            while(itr.hasNext()){
                SelectionKey check = itr.next();
                if(check.channel() instanceof SocketChannel){
                      client = (SocketChannel) check.channel();
                      if(client == sender.sender) continue;
                }else continue;

                write.flip();
                try {
                    client.write(write); // Does not remove the bytes from the buffer while writng but only reads them into the channel
                } catch (IOException e) {
                    // Client might have closed the connection or either some other connection so its better to close the channel itself!
                    SelectionKey key = (SelectionKey) check;
                    key.cancel();
                    try {
                        client.close(); // Important cuz the socket channel from our side is still open
                    } catch (IOException ex) {}
                }
            }
        }
    }
}
