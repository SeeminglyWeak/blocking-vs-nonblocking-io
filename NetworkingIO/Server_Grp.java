package NetworkingIO;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

//Wrapper for the Queue
class message{
    String msg;
    Socket ref;

    message(String msg){
        this.msg = msg;
    }

    message(String msg,Socket ref){
        this.msg = msg;
        this.ref = ref;
    }
}

//Thread for the message transfer
class Server_out implements Runnable{
    Thread t;

    Server_out(){
        t = new Thread(this);
        t.start();
    }

    @Override
    public void run(){
        while(true){
            message msg;
            try{
                msg = Server_in.messages.take();
            } catch (InterruptedException e) {
                System.err.println(e);
                msg = new message("Message could not be delivered");
            }
            for(Socket s : Server.clients){
                if(s == msg.ref) continue;

                Server.writers.get(s).println(msg.msg);
            }
        }
    }
}

class Server_in implements Runnable{
    static BlockingQueue<Socket> client = new LinkedBlockingQueue<>();
    static BlockingQueue<message> messages = new LinkedBlockingQueue<>();
    Thread t;
    Socket ref;

    Server_in(){
        t = new Thread(this,"Producer");
        t.start();
    }

    Server_in(Socket ref){
        this.ref = ref;
        t = new Thread(this);
        t.start();
    }

    @Override
    //Modification to be made to save the name of the user and display his name when he messages!
    public void run(){

        if(Thread.currentThread().getName().equals("Producer")){
            while(true){
                try{
                    new Server_in(client.take());
                } catch (InterruptedException e) {
                    System.err.println(e);
                }
            }
        }

        InputStreamReader in;
        BufferedReader reader = null;

        try{
            in = new InputStreamReader(ref.getInputStream());
            reader = new BufferedReader(in);
        } catch (IOException i) {
            System.err.println(i);
        }

        String line;
        try {
            while ((line = reader.readLine()) != null) { //Modification to made to specify the other users to notify them who left!
                if(line.equals("exit0")) break;
                else messages.put(new message(line, ref));
            }
        } catch (IOException | InterruptedException e) {
            System.err.println(e);
        }

    }

}
