package RED.Lab5;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Scanner;

public class EchoServer {
    protected static final int THREADS = 10;
    protected static final String logfile = "/home/carlos/javalog.txt";

    public static void main(String[] args) throws IOException, InterruptedException {
        ServerSocket ss = new ServerSocket(7777);
        String[] logs = new String[THREADS];
        EchoSocket[] th = new EchoSocket[THREADS];
        WaitThread wt = new WaitThread(th, logs);
        wt.start();
        while(!ss.isClosed()) {
            //Check free position
            int position = -1;
            //loop until some client disconnects
            for (int i = 0; i < THREADS; i++) {
                if (th[i] == null) {
                    position = i;
                    break;
                }
            }
            if (position != -1) {
                th[position] = new EchoSocket(ss.accept(), logs, position);
                th[position].start();
            }
        }
    }
}

class WaitThread extends Thread {
    String[] logs;
    EchoSocket[] th;

    WaitThread(EchoSocket[] th, String[] logs) {
        this.th = th;
        this.logs = logs;
    }

    @Override
    public void run() {
        try {
            PrintWriter pw;
            //Free space if necessary
            while (true) {
                pw = new PrintWriter(
                        new FileOutputStream(EchoServer.logfile, true),
                        true
                );
                for (int i = 0; i < EchoServer.THREADS; i++) {
                    //check if ready, then wait
                    if (th[i] != null && !th[i].isAlive()) {
                        th[i].join();
                        pw.printf(logs[i]);
                        th[i] = null;
                    }
                }
                pw.close();
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}

class EchoSocket extends Thread {
    protected Socket s;
    private static final String QUIT = "QUIT";
    private static final String SOURCE = "https://github.com/kauron/ETSINF-2/blob/master/src/RED/Lab5/EchoServer.java";
    private String[] log;
    private int position;

    EchoSocket(Socket s, String[] log, int position) {
        this.s = s;
        this.log = log;
        this.position = position;
    }

    @Override
    public void run() {
        try {
            //initial setup
            Scanner input = new Scanner(s.getInputStream());
            PrintWriter output = new PrintWriter(s.getOutputStream(), true);
            String echo;
            //welcome to the server
            output.println("My cool raspberry echo server");
            output.println("Source code at: " + SOURCE);
            output.println("To exit type " + QUIT);
            output.flush();
            //print date and user ip for entrance
            DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            Calendar cal = Calendar.getInstance();
            String dateLog = dateFormat.format(cal.getTime());
            log[position] =  String.format("----ENTER----\t\t%s\t\t%s\n", dateLog, s.getRemoteSocketAddress());
            //begin scanning
            while (!(echo = input.nextLine()).equals(QUIT)) {
                log[position] += echo + '\n';
                output.println(echo);
                output.flush();
            }
            //print date and user ip for exit
            cal = Calendar.getInstance();
            dateLog = dateFormat.format(cal.getTime());
            log[position] += String.format("-----EXIT----\t\t%s\t\t%s\n", dateLog, s.getRemoteSocketAddress());
            //close connections
            output.close();
            input.close();
            s.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
