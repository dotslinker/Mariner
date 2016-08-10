package it.dongnocchi.mariner;

import java.util.Comparator;
import java.util.PriorityQueue;

/**
 * Created by Paolo on 08/03/2016.
 */
public class RunningMedian {

    Float[] queue;
    int queue_counter;
    PriorityQueue<Float> lower, higher;

    public RunningMedian(int size) {

        // Two priority queues, one of reversed order.
        lower = new PriorityQueue<Float>(size,
                new Comparator<Float>() {
                    public int compare(Float arg0, Float arg1) {
                        return (arg0 < arg1) ? 1 : arg0 == arg1 ? 0 : -1;
                    }
                });

        higher = new PriorityQueue<Float>();

        queue = new Float[size];
        queue_counter = 0;
    }

    public float UpdateRunningMedian(float new_val)
    {
        float act_value;
        float old_value;

        old_value = queue[queue_counter];
        queue[queue_counter] = new_val;

        insert(new_val);

        act_value = getMedian();

        remove( old_value);

        return act_value;
    }


    public void insert(Float n) {
        if (lower.isEmpty() && higher.isEmpty())
            lower.add(n);
        else {
            if (n <= lower.peek())
                lower.add(n);
            else
                higher.add(n);
            rebalance();
        }
    }

    void rebalance() {
        if (lower.size() < higher.size() - 1)
            lower.add(higher.remove());
        else if (higher.size() < lower.size() - 1)
            higher.add(lower.remove());
    }

    public Float getMedian() {
        if (lower.isEmpty() && higher.isEmpty())
            return null;
        else if (lower.size() == higher.size())
            return (lower.peek() + higher.peek()) / 2;
        else
            return (lower.size() < higher.size()) ? higher.peek() : lower
                    .peek();
    }

    public void remove(Float n) {
        if (lower.remove(n) || higher.remove(n))
            rebalance();
    }
}