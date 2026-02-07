package NetworkingNIO;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

public class Server {
    static void accepting_client(SelectionKey key){
        ServerSocketChannel server = (ServerSocketChannel) key.channel();
        Selector selector = key.selector();

        try {
            SocketChannel client = server.accept();
            client.configureBlocking(false);
            client.register(selector,SelectionKey.OP_READ, ByteBuffer.allocate(1024 * 64));
            System.out.println("Client accepted from address -> " + client.getRemoteAddress());
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Failed attempt to let a client join");
        }
    }

    public static void main(String[] args) {
        Selector selector;
        ServerSocketChannel server;

        try {
            selector = Selector.open();

            server = ServerSocketChannel.open();
            server.bind(new InetSocketAddress(Integer.parseInt(args[0])));
            System.out.println("Server started on port number -> " + args[0]);
            server.configureBlocking(false);
            // Runtime error if not configured beforehand as non-blocking!
            server.register(selector, SelectionKey.OP_ACCEPT); // Will throw an error if the server/client is blocking, and we try to register it with a selector!

            new server_Write(selector); //Starting the write thread!
            new server_Read(); //Starting the read thread!
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("The server could not be started!!");
            return;
        }

        try{
            while(true) {
                int readyKeys = selector.select(); //Only updates the set of the selection keys and blocks the thread currently executing it!

                if (readyKeys == 0) continue;

                System.out.println("Number of ready ops -> " + readyKeys);

                Iterator<SelectionKey> itr = selector.selectedKeys().iterator();
                while(itr.hasNext()){
                    SelectionKey key = itr.next();

                    if(key.isAcceptable()){
                        //Accepting the client join request by the main thread
                        System.out.println("Accepting a client!");
                        accepting_client(key);
                    }else if(key.isReadable()){
                        //Giving the work of reading from the specific client to another thread
                        try {
                            System.out.println("Reading from a client!");
                            server_Read.keys.put(key);
                        } catch (InterruptedException e) {
                            // Will decide the fail safe after!
                            e.printStackTrace();
                        }
                    }

                    itr.remove();
                }
            }
        } catch (IOException e) {
            // Will decide the fail safe after!
            e.printStackTrace();
        }
    }
}

/*
Bugs Encountered :
1. Did not set the blocking mode of the new client joining
2. Telnet sending each char one by one and not by enter
3.
*/