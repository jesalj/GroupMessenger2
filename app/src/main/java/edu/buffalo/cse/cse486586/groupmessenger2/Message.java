package edu.buffalo.cse.cse486586.groupmessenger2;

import java.io.Serializable;

/**
 * Created by jesal on 3/11/15.
 */
public class Message implements Serializable {

    private String msgId;  // unique message suggest
    private int suggested;
    private int port;    // unique port number
    private int agreed;    // message priority
    private String msgType;
    private String data; // message to be delivered
    private boolean deliverable; // message deliverable or not

    public Message() {
        msgType = "";
        data = "";
        deliverable = false;
    }

    public Message(String m, int i, int pt, int p, String t, String d, boolean del) {
        msgId = m;
        suggested = i;
        port = pt;
        agreed = p;
        msgType = t;
        data = d;
        deliverable = del;
    }


    public int getSuggested() {
        return suggested;
    }

    public void setSuggested(int i) {
        suggested = i;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int p) {
        port = p;
    }

    public int getAgreed() {
        return agreed;
    }

    public void setAgreed(int pr) {
        agreed = pr;
    }

    public String getData() {
        return data;
    }

    public void setData(String dt) {
        data = dt;
    }

    public boolean isDeliverable() {
        return deliverable;
    }

    public void setDeliverable(boolean del) {
        deliverable = del;
    }

    public String getMsgType() {
        return msgType;
    }

    public void setMsgType(String mt) {
        msgType = mt;
    }

    public String getMsgId() {
        return msgId;
    }

    public void setMsgId(String mid) {
        msgId = mid;
    }
}
