package pt.tecnico.distledger.server.domain;

import java.util.ArrayList;

public class VectorClock {

    private final ArrayList<Integer> timeStamps;

    public VectorClock() {
        timeStamps = new ArrayList<>();
    }

    public VectorClock(ArrayList<Integer> timeStamps) { this.timeStamps = timeStamps; }

    public Integer getTS(Integer i) {
        return timeStamps.get(i);
    }

    public ArrayList<Integer> getFullTs() { return timeStamps; }

    public void setTS(Integer i, Integer value) {
        timeStamps.set(i, value);
    }

    // Greater or equal
    public boolean GE(VectorClock v) {
        for (int i = 0; i < timeStamps.size(); i++) {
            if (timeStamps.get(i) < v.getTS(i)) { return false; }
        }
        return true;
    }
}
