package com.leastfixedpoint.json.examples;

import com.leastfixedpoint.json.JSONReader;
import com.leastfixedpoint.json.JSONWriter;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * A simple JSON "echo server" that interactively reads JSON values from
 * arriving TCP connections, prints them to stdout, and immediately echoes
 * them back again to the caller&mdash;but pretty-printed.
 *
 * Demonstrates the streaming ability of {@link JSONReader}.
 */
public class JSONEchoServer implements Runnable {
    private final Socket sock;

    public JSONEchoServer(Socket connectedSocket) {
        this.sock = connectedSocket;
    }

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 45678;
        System.out.println("Creating listening socket on port " + port + "...");
        ServerSocket s = new ServerSocket(port);
        while (true) {
            Socket c = s.accept();
            System.out.println("Accepted connection " + c);
            new Thread(new JSONEchoServer(c)).start();
        }
    }

    @Override
    public void run() {
        try {
            JSONReader r = new JSONReader(new InputStreamReader(this.sock.getInputStream()));
            JSONWriter w = new JSONWriter(new OutputStreamWriter(this.sock.getOutputStream()), true);

            while (true) {
                try {
                    Object blob = r.read();
                    System.out.println(JSONWriter.writeToString(blob, true));
                    w.write(blob);
                    w.getWriter().write("\n");
                    w.getWriter().flush();
                } catch (EOFException ee) {
                    break;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
	    try {
		this.sock.close();
	    } catch (IOException e) {
		e.printStackTrace();
	    }
	}
        System.out.println("Connection terminated " + this.sock);
    }
}
