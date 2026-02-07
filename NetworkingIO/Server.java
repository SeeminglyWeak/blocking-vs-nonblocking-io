package Networking_Project.NetworkingIO;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class Server implements Runnable{
    Thread t;
    ServerSocket ref;
    static List<Socket> clients = new CopyOnWriteArrayList<>();
    static Map<Socket, PrintWriter> writers = new ConcurrentHashMap<>();

    Server(ServerSocket ref){
        this.ref = ref;
        t = new Thread(this);
    }

    public void run(){
        while(true){
            try{
                Socket client = ref.accept();
                clients.add(client);
                writers.put(client,new PrintWriter(new OutputStreamWriter(client.getOutputStream()),true));
                Server_in.client.put(client);
                System.out.println("CLIENT ACCEPTED FROM " + client.getRemoteSocketAddress());
            } catch (IOException i) {
                System.err.println(i);
                //No fallback right now!
            } catch (InterruptedException e) {
                System.err.println(e);
                //No fallback right now!
            }
        }
    }

    public static void main(String[] args) {
        ServerSocket server;

        try{
            server = new ServerSocket(Integer.parseInt(args[0]));
            System.out.println("Server Started!");
            new Server_out();
        } catch (IOException e) {
            System.err.println(e);
            System.out.println("Try another port number!");
            return;
        }

        new Server_in(); //Object to start making new threads for reading from client
        Server client_acceptor = new Server(server); //New object because I would need it be scalable in future if anything else

        client_acceptor.t.start();

        try{
            client_acceptor.t.join();
        } catch (InterruptedException e) {
            System.err.println(e);
        }finally{
            System.out.println("Server closed!");
            //Should also broadcast the message to everyone who is in the group chat before closing
      }
    }
}
