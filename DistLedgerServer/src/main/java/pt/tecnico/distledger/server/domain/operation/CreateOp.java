package pt.tecnico.distledger.server.domain.operation;

import pt.tecnico.distledger.server.domain.VectorClock;

public class CreateOp extends Operation {

    public CreateOp(String account) {
        super(account);
    }

    public CreateOp(String account, VectorClock prev, VectorClock TS, boolean stable, String type) {
        super(account, prev, TS, stable, type);
    }

    public String toString() {
        return "type:  OP_CREATE_ACCOUNT\n" + "userId: " + getAccount()+ "\n";
    }

}
