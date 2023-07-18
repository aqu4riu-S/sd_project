package pt.tecnico.distledger.server.domain.operation;

import pt.tecnico.distledger.server.domain.VectorClock;

public class Operation {
    private String account;

    private Boolean stable = false;

    private VectorClock prev = new VectorClock();

    private VectorClock TS = new VectorClock();

    private String type;

    public Operation(String fromAccount) {
        this.account = fromAccount;
    }

    public Operation(String fromAccount, VectorClock prev, VectorClock TS, boolean stable, String type) {
        this.account = fromAccount;
        this.prev = prev;
        this.TS = TS;
        this.stable = stable;
        this.type = type;
    }

    public Operation(String fromAccount, VectorClock prev, VectorClock TS, boolean stable, String type, String destAccount, int amount) {
        this.account = fromAccount;
        this.prev = prev;
        this.TS = TS;
        this.stable = stable;
        this.type = type;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public Boolean isStable() {
        return this.stable;
    }

    public void setStable(Boolean stable) {
        this.stable = stable;
    }

    public VectorClock getPrev() {
        return prev;
    }

    public void setPrev(VectorClock prev) {
        this.prev = prev;
    }

    public VectorClock getTS() {
        return TS;
    }

    public void setTS(VectorClock TS) {
        this.TS = TS;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
