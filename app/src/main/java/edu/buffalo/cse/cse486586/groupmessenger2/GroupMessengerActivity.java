package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 * 
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {

    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    static final String DEBUG = "DEBUG";
    static final Uri providerUri = Uri.parse("content://edu.buffalo.cse.cse486586.groupmessenger2.provider");
    static final int SERVER_PORT = 10000;
    static int N = 5;
    static final String REMOTE_PORT0 = "11108";
    static final String REMOTE_PORT1 = "11112";
    static final String REMOTE_PORT2 = "11116";
    static final String REMOTE_PORT3 = "11120";
    static final String REMOTE_PORT4 = "11124";
    static final ArrayList<String> remotePorts = new ArrayList<>();
    static TreeMap<String, Message> waitQueue = new TreeMap<>();
    static SortedSet<TreeMap.Entry<String, Message>> priorityQueue = new TreeSet<>(new MessageComparator());
    static int port = -1;
    static HashMap<String, Integer[]> allIn = new HashMap<>();
    static int dbKey = 0;
    static int suggestedPriority = 0;
    static int agreedPriority = 0;
    static int msgId = 0;
    static ServerSocket serverSocket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        remotePorts.add(REMOTE_PORT0);
        remotePorts.add(REMOTE_PORT1);
        remotePorts.add(REMOTE_PORT2);
        remotePorts.add(REMOTE_PORT3);
        remotePorts.add(REMOTE_PORT4);

        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));
        switch (myPort) {
            case REMOTE_PORT0:
                port = 0;
                break;
            case REMOTE_PORT1:
                port = 1;
                break;
            case REMOTE_PORT2:
                port = 2;
                break;
            case REMOTE_PORT3:
                port = 3;
                break;
            case REMOTE_PORT4:
                port = 4;
                break;
            default:
                break;
        }

        try {
            /*
             * Create a server socket as well as a thread (AsyncTask) that listens on the server
             * port.
             *
             * AsyncTask is a simplified thread construct that Android provides. Please make sure
             * you know how it works by reading
             * http://developer.android.com/reference/android/os/AsyncTask.html
             */
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            /*
             * Log is a good way to debug your code. LogCat prints out all the messages that
             * Log class writes.
             *
             * Please read http://developer.android.com/tools/debugging/debugging-projects.html
             * and http://developer.android.com/tools/debugging/debugging-log.html
             * for more information on debugging.
             */
            Log.e(TAG, "Can't create a ServerSocket");
            return;
        }

        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());
        
        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));
        
        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */
        final EditText editText = (EditText) findViewById(R.id.editText1);
        final Button send = (Button) findViewById(R.id.button4);

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = editText.getText().toString() + "\n";
                editText.setText("");
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "init", msg, myPort);
            }
        });

        /*editText.setOnKeyListener (new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    String msg = editText.getText().toString() + "\n";
                    editText.setText("");
                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "init", msg, myPort);

                    return true;
                }
                return false;
            }
        });*/

        long delay = 10000;
        long period = 1000;
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "dummy", myPort);
            }
        };
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(timerTask, delay, period);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }

    @Override
    protected void onPause() {
        try {
            serverSocket.close();
            Log.d(TAG, "serverSocket closed on port: " + port);
        } catch (IOException e) {
            Log.e(TAG, "IOException in closing serverScoket in onDestroy()", e);
        }
    }

    @Override
    protected void onStop() {
        if (!serverSocket.isClosed()) {
            try {
                serverSocket.close();
                Log.d(TAG, "serverSocket closed on port: " + port);
            } catch (IOException e) {
                Log.e(TAG, "IOException in closing serverScoket in onDestroy()", e);
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (!serverSocket.isClosed()) {
            try {
                serverSocket.close();
                Log.d(TAG, "serverSocket closed on port: " + port);
            } catch (IOException e) {
                Log.e(TAG, "IOException in closing serverScoket in onDestroy()", e);
            }
        }
    }

    /***
     * ServerTask is an AsyncTask that should handle incoming messages. It is created by
     * ServerTask.executeOnExecutor() call in SimpleMessengerActivity.
     *
     * Please make sure you understand how AsyncTask works by reading
     * http://developer.android.com/reference/android/os/AsyncTask.html
     *
     * @author stevko
     *
     */
    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            serverSocket = sockets[0];
            Socket client = null;
            HashMap<Socket, Integer> clientPort = new HashMap<>();
            System.out.println("---- THIS PORT: " + port + " ----");

            while (!serverSocket.isClosed()) {

                try {
                    client = serverSocket.accept();
                    ObjectInputStream ois = new ObjectInputStream(client.getInputStream());
                    Message msg = (Message) ois.readObject();
                    clientPort.put(client, msg.getPort());
                    if (!msg.getMsgType().equals("dummy")) {
                        publishProgress (
                                msg.getMsgId(),
                                Integer.toString(msg.getSuggested()),
                                Integer.toString(msg.getPort()),
                                Integer.toString(msg.getAgreed()),
                                msg.getMsgType(),
                                msg.getData(),
                                Boolean.toString(msg.isDeliverable())
                        );
                    }
                } catch (IOException ioe) {
                    Log.e(TAG, "IOException in ServerTask(): " + clientPort.get(client), ioe);
                    //remotePorts.set(clientPort.get(client), null);
                    hasFailed(clientPort.get(client), "servertask", "srvr");
                } catch (ClassNotFoundException e) {
                    Log.e(TAG, "ClassNotFoundException in ServerTask(): ", e);
                }
            }

            return null;
        }

        protected void onProgressUpdate(String...strings) {
            /*
             * The following inserts what is received in doInBackground() into sqlite database.
             */
            String mId = strings[0];
            int mSugg = Integer.parseInt(strings[1]);
            int mPort = Integer.parseInt(strings[2]);
            int mAgr = Integer.parseInt(strings[3]);
            String mType = strings[4];
            String mData = strings[5];
            //boolean mDeliverable = Boolean.parseBoolean(strings[6]);
            Message msg;

            if (mType.equals("init")) {
                suggestedPriority = Math.max(suggestedPriority, agreedPriority) + 1;
                msg = new Message(mId, suggestedPriority, mPort, -1, "suggest", mData, false);
                waitQueue.put(mId, msg);
                System.out.println("---- waitQueue ----");
                for (String mi : waitQueue.keySet()) {
                    System.out.println("id:" + mi + ", del:" + waitQueue.get(mi).isDeliverable());
                }
                System.out.println("-------------------");

                strings[1] = Integer.toString(suggestedPriority);
                strings[3] = Integer.toString(-1);
                strings[4] = "suggest";
                strings[6] = Boolean.toString(false);

                // unicast suggested priority response
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,
                        strings[0],
                        strings[1],
                        strings[2],
                        strings[3],
                        strings[4],
                        strings[5],
                        strings[6],
                        Integer.toString(mPort)
                );

            } else if (mType.equals("suggest")) {

                int allInCount;
                int highestSeen;
                //if (allIn.containsKey(mId)) {
                    allInCount = allIn.get(mId)[0] + 1;
                    highestSeen = Math.max(mSugg, allIn.get(mId)[1]);
                    allIn.put(mId, new Integer[]{allInCount, highestSeen});
                /*} else {
                    allInCount = 1;
                    highestSeen = mSugg;
                    allIn.put(mId, new Integer[]{allInCount, highestSeen});
                }*/

                if (allIn.get(mId)[0] == N) {
                    System.out.println("-- ALL IN | id:" + mId + ", highestSeen:" + highestSeen + " --");
                    agreedPriority = highestSeen;
                    strings[0] = mId;
                    strings[1] = Integer.toString(-1);
                    strings[2] = Integer.toString(mPort);
                    strings[3] = Integer.toString(highestSeen);
                    strings[4] = "final";
                    strings[5] = mData;
                    strings[6] = Boolean.toString(true);
                    // multicast final agreed message to all ports
                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,
                            strings[0],
                            strings[1],
                            strings[2],
                            strings[3],
                            strings[4],
                            strings[5],
                            strings[6],
                            "multi"
                    );
                }

            } else if (mType.equals("final")) {

                System.out.println("-- FINAL | id:" + mId + ", pr:" + mAgr + " --");
                msg = waitQueue.get(mId);
                msg.setDeliverable(true);
                msg.setAgreed(mAgr);
                waitQueue.put(mId, msg);
                agreedPriority = Math.max(agreedPriority, mAgr);

                priorityQueue.addAll(waitQueue.entrySet());

                Iterator<TreeMap.Entry<String, Message>> itr = priorityQueue.iterator();
                TreeMap.Entry<String, Message> curr;
                boolean first = true;

                while (itr.hasNext() && (curr = itr.next()).getValue().isDeliverable()) {
                    if (first) {
                        System.out.println("---- priorityQueue ----");
                        first = false;
                    }
                    Message mp = curr.getValue();
                    waitQueue.remove(mp.getMsgId());
                    ContentValues storeMsg = new ContentValues();
                    storeMsg.put("key", Integer.toString(dbKey));
                    storeMsg.put("value", mp.getData());
                    System.out.println("id:" + mp.getMsgId() + ", pr:" + mp.getAgreed() +
                            ", del:" + mp.isDeliverable() + ", dbKey:" + dbKey);
                    dbKey++;
                    getContentResolver().insert(providerUri, storeMsg);

                    TextView textView1 = (TextView) findViewById(R.id.textView1);
                    textView1.append(mp.getData() + "\t\n");
                }
                if (!first) {
                    while (itr.hasNext()) {
                        Message mp = itr.next().getValue();
                        System.out.println("id:" + mp.getMsgId() + ", pr:" + mp.getAgreed() +
                                ", del:" + mp.isDeliverable());
                    }
                    System.out.println("-----------------------");
                }

                priorityQueue.clear();
            }
            return;
        }
    }

    /***
     * ClientTask is an AsyncTask that should send a string over the network.
     * It is created by ClientTask.executeOnExecutor() call whenever OnKeyListener.onKey() detects
     * an enter key press event.
     *
     * @author stevko
     *
     */
    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {

            if (msgs[0].equals("dummy")) {

                String msgToSend = "dummy";
                Message m = new Message(Integer.toString(port), -1, port, -1, "dummy", msgToSend, false);
                ArrayList<Integer> failedNodes = new ArrayList<>();

                for (String remotePort : remotePorts) {
                    if (remotePort != null) {
                        try {
                            Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(remotePort));
                            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                            out.flush();
                            out.writeObject(m);
                            out.flush();
                            out.close();
                            socket.close();
                        } catch (UnknownHostException e) {
                            Log.e(TAG, "Dummy B-Multicast UnknownHostException for port: " + remotePorts.indexOf(remotePort), e);
                        } catch (IOException e) {
                            Log.e(TAG, "Dummy B-Multicast Socket IOException for port: " + remotePorts.indexOf(remotePort), e);
                            failedNodes.add(remotePorts.indexOf(remotePort));
                            hasFailed(remotePorts.indexOf(remotePort), "dummyb", "dummy");
                        }
                    }
                }

                for (int fn : failedNodes) {
                    remotePorts.set(fn, null);
                }

            } else if (msgs[0].equals("init")) {

                String msgToSend = msgs[1];
                msgId++;
                //int id = Integer.parseInt(Integer.toString(port) + Integer.toString(msgId));
                String id = Integer.toString(port) + Integer.toString(msgId);
                Message m = new Message(id, -1, port, -1, "init", msgToSend, false);
                allIn.put(id, new Integer[]{0, 0});
                ArrayList<Integer> failedNodes = new ArrayList<>();

                for (String remotePort : remotePorts) {
                    if (remotePort != null) {
                        try {
                            Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(remotePort));
                            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                            out.flush();
                            out.writeObject(m);
                            out.flush();
                            out.close();
                            socket.close();
                        } catch (UnknownHostException e) {
                            Log.e(TAG, "Init B-Multicast UnknownHostException for port: " + remotePorts.indexOf(remotePort), e);
                        } catch (IOException e) {
                            Log.e(TAG, "Init B-Multicast Socket IOException for port: " + remotePorts.indexOf(remotePort), e);
                            failedNodes.add(remotePorts.indexOf(remotePort));
                            hasFailed(remotePorts.indexOf(remotePort), "initb", m.getMsgId());
                        }
                    }
                }

                for (int fn : failedNodes) {
                    remotePorts.set(fn, null);
                }

            } else if (msgs[7].equals("multi")) {

                Message m = new Message(
                        msgs[0],
                        Integer.parseInt(msgs[1]),
                        Integer.parseInt(msgs[2]),
                        Integer.parseInt(msgs[3]),
                        msgs[4],
                        msgs[5],
                        Boolean.parseBoolean(msgs[6])
                );
                ArrayList<Integer> failedNodes = new ArrayList<>();

                for (String remotePort : remotePorts) {
                    if (remotePort != null) {
                        try {
                            Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(remotePort));
                            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                            out.flush();
                            out.writeObject(m);
                            out.flush();
                            out.close();
                            socket.close();
                        } catch (UnknownHostException e) {
                            Log.e(TAG, "Multi B-Multicast UnknownHostException for port: " + remotePorts.indexOf(remotePort), e);
                        } catch (IOException e) {
                            Log.e(TAG, "Mutli B-Multicast Socket IOException for port: " + remotePorts.indexOf(remotePort), e);
                            failedNodes.add(remotePorts.indexOf(remotePort));
                            hasFailed(remotePorts.indexOf(remotePort), "multib", m.getMsgId());
                        }
                    }
                }

                for (int fn : failedNodes) {
                    remotePorts.set(fn, null);
                }

            } else {

                Message m = new Message(
                        msgs[0],
                        Integer.parseInt(msgs[1]),
                        Integer.parseInt(msgs[2]),
                        Integer.parseInt(msgs[3]),
                        msgs[4],
                        msgs[5],
                        Boolean.parseBoolean(msgs[6])
                );

                int fPort = Integer.parseInt(msgs[7]);
                String rPort = remotePorts.get(fPort);

                if (rPort != null) {
                    try {
                        Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(remotePorts.get(fPort)));
                        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                        out.flush();
                        out.writeObject(m);
                        out.flush();
                        out.close();
                        socket.close();
                    } catch (UnknownHostException e) {
                        Log.e(TAG, "Unicast UnknownHostException for port: " + fPort, e);
                    } catch (IOException e) {
                        Log.e(TAG, "Unicast Socket IOException for port: " + fPort, e);
                        remotePorts.set(fPort, null);
                        hasFailed(fPort, "unicast", m.getMsgId());
                    }
                }
            }

            return null;
        }
    }

    public void hasFailed(int fail, String src, String msf) {

        if (!msf.equals("srvr")) {
            N--;
        }
        System.out.println("-- FAILED (N=" + N + ") | id:" + msf + ", fail:" + fail + ", src:" + src + " --");

        ArrayList<String> waits = new ArrayList<>();
        ArrayList<TreeMap.Entry<String, Message>> priors = new ArrayList<>();

        for (String mId : waitQueue.keySet()) {
            Message ms = waitQueue.get(mId);
            if (allIn.containsKey(mId)) {
                if (allIn.get(mId) != null && allIn.get(mId)[0] == N) {
                    System.out.println("-- ALL IN STUCK | id:" + mId + " --");
                    agreedPriority = allIn.get(mId)[1];
                    // multicast final agreed message to all ports
                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,
                            mId,
                            Integer.toString(-1),
                            Integer.toString(ms.getPort()),
                            Integer.toString(agreedPriority),
                            "final",
                            ms.getData(),
                            Boolean.toString(true),
                            "multi"
                    );
                }
            }
        }

        for (String id : waitQueue.keySet()) {
            Message m = waitQueue.get(id);
            if (m.getPort() == fail) {
                System.out.println("-- WAIT REMOVE | id:" + id + " --");
                waits.add(id);
                //waitQueue.remove(id);
            }
        }

        for (TreeMap.Entry<String, Message> e : priorityQueue) {
            Message m = e.getValue();
            if (m.getPort() == fail) {
                System.out.println("-- PRIORITY REMOVE | id:" + m.getMsgId() + " --");
                priors.add(e);
                //priorityQueue.remove(e);
            }
        }

        for (String rw : waits) {
            waitQueue.remove(rw);
        }
        for (TreeMap.Entry<String, Message> rp : priors) {
            priorityQueue.remove(rp);
        }

    }
}
