package edu.buffalo.cse.cse486586.groupmessenger2;

import java.util.Comparator;
import java.util.TreeMap;

/**
 * Created by jesal on 3/12/15.
 */
public class MessageComparator implements Comparator<TreeMap.Entry<String, Message>> {

    @Override
    public int compare(TreeMap.Entry<String, Message> m1, TreeMap.Entry<String, Message> m2) {
        Integer p1 = m1.getValue().getAgreed();
        Integer p2 = m2.getValue().getAgreed();
        if (p1 < p2)
            return -1;
        else if (p1 > p2)
            return 1;
        else {
            boolean del1 = m1.getValue().isDeliverable();
            boolean del2 = m2.getValue().isDeliverable();
            if (del1 && del2) {
                int pt1 = m1.getValue().getPort();
                int pt2 = m2.getValue().getPort();
                if (pt1 < pt2)
                    return -1;
                else if (pt1 > pt2)
                    return 1;
            } else if (del1 && !del2) {
                return 1;
            } else if (!del1 && del2) {
                return -1;
            }
        }
        return 0;
    }
}
