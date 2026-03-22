package Networking_Project.NetworkingNIO;

import java.io.IOException;
import java.net.Socket;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class LocalProxy {
    private static Socket server;
    static Encrypter en;
    static Decrypter de;
    static Thread main;

    public static void main(String[] args) {

        if(args.length != 2){
            System.out.println("Usage : java <Server's IP Address> <Server's Port>");
            return;
        }

        try {
            server = new Socket(args[0],Integer.parseInt(args[1]));
            de = new Decrypter(server.getInputStream());
            en = new Encrypter(server.getOutputStream());
            main = Thread.currentThread();
            de.Decrypter_thread.start();
            en.Encrypter_thread.start();
        } catch (IOException e) {
            System.out.println("Could not find the server at the address/port.");
            return;
        } catch (NumberFormatException n){
            System.out.println("The port should be a valid number!");
            return;
        }

        Scanner in = new Scanner(System.in);
        outer : while (true) {
            try {
                while (true) {
                    String input = in.nextLine();
                    if(input.equalsIgnoreCase("<<<Logout>>>")) {
                        en.Encrypter_thread.interrupt();
                        try{
                            server.close();
                        } catch (IOException ignored) {} 
                        break outer;
                    }
                    Encrypter.messages_queue.put(input);
                }
            } catch (NoSuchElementException i) {
                System.out.println("Connection closed by server!");
                break;
            }catch(InterruptedException ignored) { } // Since there is no effect of interrupting it anyway!
        }
        in.close();

    }
}