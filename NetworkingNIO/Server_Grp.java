/*
* This class serves the role of having a single thread which will keep reading the data from the other threads.
* After reading the data in the thread, it will use a buffer to pass it on to every other client connected.
*/

package Networking_Project.NetworkingNIO;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

class wrapper{
    String msg, name;
    SocketChannel sender;

    wrapper(String name,SocketChannel sender){
        this.name = name;
        this.sender = sender;
    }

    void setMsg(String data){
        data = name + " : " + data;
        this.msg = data;
    }
}

class server_Read implements Runnable{
    Thread t;
    static BlockingQueue<SelectionKey> keys = new LinkedBlockingQueue<>();
    Map<SelectionKey,wrapper> saved_clients = new HashMap<>();

    server_Read(){
        t = new Thread(this,"Reader");
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
                while((bytes = client.read(buffer)) > 0){ // Guards against the client disconnected or no data is left in the buffer
                    buffer.flip();
                    data += StandardCharsets.UTF_8.decode(buffer);
                    buffer.clear();
                }
                System.out.println("Data received -> " + data); // Debug statement!
                if(bytes == -1){
                    System.out.println("Client disconnected from -> " + client.getRemoteAddress());
                    client.close();
                    key.cancel();
                    continue;
                }

                key.interestOps(SelectionKey.OP_READ); //Reassigning the interest OP
                key.selector().wakeup();

                if(!saved_clients.containsKey(key)){
                    saved_clients.put(key,new wrapper(data.trim(),client));
                    continue;
                }

                wrapper message = saved_clients.get(key);
                message.setMsg(data);
                server_Write.data.put(message);
            } catch (InterruptedException | IOException e) {
                // Will decide the fail-safe after!
                e.printStackTrace();
            } catch (Exception e) {
                System.out.println("Some Exception occurred in server_Write");
            }
        }
    }

}

class server_Write implements Runnable{
    Thread t;
    ByteBuffer write = ByteBuffer.allocateDirect(1024 * 512);
    static BlockingQueue<wrapper> data = new LinkedBlockingQueue<>();
    Selector selector;

    server_Write(Selector selector){
        this.selector = selector;
        t = new Thread(this,"Writer");
        t.start();
    }

    @Override
    public void run(){
        while(true){
            write.clear();
            wrapper sender = null;
            try {
                sender = data.take();
                System.out.println("Data received by the write thread in queue -> " + sender.msg);
                write.put(sender.msg.getBytes(StandardCharsets.UTF_8));
            } catch (InterruptedException e) {
                e.printStackTrace();
                //No fail-safe right now!
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
                    client.write(write); // Does not remove the bytes from the buffer while writing but only reads them into the channel
                } catch (IOException e) {
                    // Client might have closed the connection or either some other connection so it's better to close the channel itself!
                    check.cancel();
                    try {
                        client.close(); // Important cuz the socket channel from our side is still open
                    } catch (IOException ex) {}
                } catch (Exception e) {
                    System.out.println("Some Exception occurred in server_Write");
                }
            }
        }
    }
}
