package pt.tecnico.distledger.server.domain.operation;

public class DeleteOp extends Operation {

    public DeleteOp(String account) {
        super(account);
    }

    public String toString() {
        return "type:  OP_DELETE_ACCOUNT\n" + "userId: " + getAccount()+ "\n";
    }
}
