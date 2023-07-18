package pt.tecnico.distledger.server.domain;


import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import pt.tecnico.distledger.server.ServerMain;
import pt.tecnico.distledger.server.domain.operation.CreateOp;
import pt.tecnico.distledger.server.domain.operation.Operation;
import pt.tecnico.distledger.server.domain.operation.TransferOp;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.CrossServerDistLedger;

import java.util.*;
import java.util.concurrent.TimeUnit;

import pt.ulisboa.tecnico.distledger.contract.distledgerserver.CrossServerDistLedger.*;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.DistLedgerCrossServerServiceGrpc;


import java.util.concurrent.TimeUnit;

import static pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions.OperationType.*;


public class ServerState {

    static int currId = 0;
    private final String broker = "broker";
    private final String secondaryQualifier = "B";
    private final String serviceName = "DistLedger";
    private final String host = "localhost";
    private final String port = "5001";
    private final String target = host + ":" + port;
    private final List<Operation> ledger;

    private final Map<String, Integer> accounts;

    private boolean isActive;

    private boolean isPrimary;

    private List<Operation> propagateBuffer;

    private DistLedgerCrossServerServiceGrpc.DistLedgerCrossServerServiceBlockingStub stub;

    private static boolean debug;

    final int deadlineMs = 3000;

    private final int numServers = 2;

    private int id;

    private VectorClock replicaTS = new VectorClock();

    private VectorClock valueTS = new VectorClock();

    public ServerState() {
        this.ledger = new ArrayList<>();
        this.accounts = new HashMap<>();
        this.accounts.put(broker, 1000);
        this.isActive = true;
        this.isPrimary = false;
        debug = false;
        this.propagateBuffer = new ArrayList<>();
        this.stub = null;
        this.id = currId;
        currId++;
    }

    private void createStub(String serverAddress) {
        ManagedChannel channel = ManagedChannelBuilder.forTarget(serverAddress).usePlaintext().build();
        stub = DistLedgerCrossServerServiceGrpc.newBlockingStub(channel);
    }

    public void checkPrimary() throws RuntimeException {
        if (!isPrimary) {
            throw new StatusRuntimeException(Status.FAILED_PRECONDITION.withDescription("Cannot write to secondary server."));
        }
    }

    public boolean checkSecondaryStatus() throws RuntimeException {
        ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
        DistLedgerCrossServerServiceGrpc.DistLedgerCrossServerServiceBlockingStub lookUpStub = DistLedgerCrossServerServiceGrpc.newBlockingStub(channel);
        LookupRequest lookupRequest = LookupRequest.newBuilder().setServiceName(serviceName).build();
        LookupResponse lookupResponse = lookUpStub.lookup(lookupRequest);
        List<NamingServerEntry> entries = lookupResponse.getServerEntriesList();
        Optional<NamingServerEntry> nse = entries.stream().filter(entry -> entry.getServerQualifier().compareTo(secondaryQualifier) == 0).findAny();
        if (nse.isPresent()) {
            String serverAddress = nse.get().getServerAddress();

            try {
                ManagedChannel s_channel = ManagedChannelBuilder.forTarget(serverAddress).usePlaintext().build();
                DistLedgerCrossServerServiceGrpc.DistLedgerCrossServerServiceBlockingStub stub = DistLedgerCrossServerServiceGrpc.newBlockingStub(s_channel);

                IsAvailableRequest request = IsAvailableRequest.newBuilder().build();
                IsAvailableResponse response = stub.isAvailable(request);

                return response.getIsAvailable();
            } catch (StatusRuntimeException e) {
                System.out.println(e.getMessage());
            }
        } else {
            throw new StatusRuntimeException(Status.FAILED_PRECONDITION.withDescription("No secondary servers available."));
        }
        return false;
    }

    public VectorClock getValueTS() { return this.valueTS; }

    public boolean isPrimary() {
        return isPrimary;
    }

    public void setPrimary(boolean primary) {
        this.isPrimary = primary;
    }

    public void setActive(boolean active) {
        this.isActive = active;
    }

    public void addLedgerEntry(Operation op) {
        ledger.add(op);
    }

    public void setDebug(boolean new_debug) {
        debug = new_debug;
    }

    public int getBalance(String accountName) {
        return accounts.get(accountName);
    }

    public void addAccount(String accountName) {
        accounts.put(accountName, 0);
    }

    public void removeAccount(String accountName) {
        accounts.remove(accountName);
    }

    public void isActive() throws RuntimeException {
        if (!isActive) {
            throw new StatusRuntimeException(Status.UNAVAILABLE.withDescription("UNAVAILABLE"));
        }
    }

    public boolean isAvailable() {
        return this.isActive;
    }

    private static void debug(String debugMessage) {
        if (debug)
            System.err.println(debugMessage);
    }

    public int getId() { return this.id; }

    //Methods related with grpc service
    //Admin methods
    public synchronized void activate() {
        debug("Activate");
        if (!isActive) {
            setActive(true);
        }
    }

    public synchronized void deactivate() {
        debug("Deactivate");
        if (isActive) {
            setActive(false);
        }
    }

    public synchronized List<Operation> getLedgerState(VectorClock prev) {
        isActive();
        debug("GetledgerState called");

        if (!valueTS.GE(prev)) {
            // error
            throw new StatusRuntimeException(Status.FAILED_PRECONDITION.withDescription("Replica is outdated (valueTS < prev)."));
        }
        return ledger;
    }

    //User methods
    public synchronized void createAccount(String accountName, VectorClock prev) throws RuntimeException {
        isActive();
        checkPrimary();

        if (!checkSecondaryStatus()) {
            throw new StatusRuntimeException(Status.FAILED_PRECONDITION.withDescription("Secondary server unavailable."));
        }

        // Updates replicaTS and computes opTS
        VectorClock opTS = writeRequest(prev);

        // Create and add op to ledger
        Operation op = new CreateOp(accountName);
        addLedgerEntry(op);
        addToPropagateBuffer(op);

        // Compares valueTS with prev
        if (this.valueTS.GE(prev)) {
            op.setStable(true);
        }

        // Executes op if it is stable
        if (op.isStable()) {
            if (accountName.equals(broker)) {
                throw new StatusRuntimeException(Status.FAILED_PRECONDITION.withDescription("Broker account cannot be created"));
            }
            if (!accounts.containsKey(accountName)) {
                // Executes op
                addAccount(accountName);
                // Updates valueTS
                mergeTS(this.valueTS, opTS);
                debug("Added Account:" + accountName);
            } else {
                throw new StatusRuntimeException(Status.ALREADY_EXISTS.withDescription("Account already exists"));
            }
        }
    }

    public synchronized int balance(String accountName, VectorClock prev) throws RuntimeException {
        isActive();

        if (!valueTS.GE(prev)) {
            // error
            throw new StatusRuntimeException(Status.FAILED_PRECONDITION.withDescription("Replica is outdated (valueTS < prev)."));
        }
        if (accounts.containsKey(accountName)) {
            int balance = getBalance(accountName);
            debug("Balance for account " + accountName + " is: " + balance);
            return balance;
        } else {
            throw new StatusRuntimeException(Status.NOT_FOUND.withDescription("Account doesn't exist"));
        }

    }

    public synchronized void transferTo(String sender, String receiver, int amount, VectorClock prev) throws RuntimeException {
        checkPrimary();

        if (!checkSecondaryStatus()) {
            throw new StatusRuntimeException(Status.FAILED_PRECONDITION.withDescription("Secondary server unavailable."));
        }

        // Updates replicaTS and computes opTS
        VectorClock opTS = writeRequest(prev);

        // Create and add op to ledger
        Operation op = new TransferOp(sender, receiver, amount);
        addLedgerEntry(op);
        addToPropagateBuffer(op);

        // Compares valueTS with prev
        if (this.valueTS.GE(prev)) {
            op.setStable(true);
        }

        if (op.isStable()) {
            if (isActive) {
                if (accounts.containsKey(sender)) {
                    if (accounts.containsKey(receiver)) {
                        if (sender.equals(receiver)) {
                            throw new StatusRuntimeException(Status.FAILED_PRECONDITION.withDescription("Sender and receiver must be different"));
                        }
                        if (amount <= 0) {
                            throw new StatusRuntimeException(Status.FAILED_PRECONDITION.withDescription("Amount must be positive"));
                        }
                        if (getBalance(sender) >= amount) {
                            accounts.put(sender, getBalance(sender) - amount);
                            accounts.put(receiver, getBalance(receiver) + amount);
                            mergeTS(this.valueTS, opTS);
                            debug("Transfer:\namount: " + amount + "\nsender: " + sender + "\n receiver: " + receiver);
                        } else {
                            throw new StatusRuntimeException(Status.FAILED_PRECONDITION.withDescription("Sender has insufficient balance for this transfer"));
                        }
                    } else {
                        throw new StatusRuntimeException(Status.NOT_FOUND.withDescription("Receiver does not exist"));
                    }
                } else {
                    throw new StatusRuntimeException(Status.NOT_FOUND.withDescription("Sender does not exist"));
                }
            }
        }
    }

    public synchronized void gossip() throws RuntimeException {

        ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
        DistLedgerCrossServerServiceGrpc.DistLedgerCrossServerServiceBlockingStub lookUpStub = DistLedgerCrossServerServiceGrpc.newBlockingStub(channel);
        LookupRequest lookupRequest = LookupRequest.newBuilder().setServiceName(serviceName).build();
        LookupResponse response = lookUpStub.lookup(lookupRequest);
        List<NamingServerEntry> entries = response.getServerEntriesList();
        String serverAddress = null;
        for (NamingServerEntry nse : entries) {
            if (nse.getServerQualifier().compareTo(secondaryQualifier) == 0) {
                serverAddress = nse.getServerAddress();

            }
        }

        // criar stub a chamar o naming server
        if (serverAddress != null) {
            createStub(serverAddress);
        }

        // fazer o propagateState request
        List<DistLedgerCommonDefinitions.Operation> list = new ArrayList<DistLedgerCommonDefinitions.Operation>();
        for (pt.tecnico.distledger.server.domain.operation.Operation op : propagateBuffer) {

            DistLedgerCommonDefinitions.VectorClock prev = DistLedgerCommonDefinitions.VectorClock.newBuilder()
                    .addAllTs(op.getPrev().getFullTs())
                    .build();

            DistLedgerCommonDefinitions.VectorClock TS = DistLedgerCommonDefinitions.VectorClock.newBuilder()
                    .addAllTs(op.getTS().getFullTs())
                    .build();

            DistLedgerCommonDefinitions.OperationType op_type;
            String userId = op.getAccount();
            DistLedgerCommonDefinitions.Operation msg_op;
            if (op instanceof TransferOp) {
                op_type = OP_TRANSFER_TO;
                int amount = ((TransferOp) op).getAmount();
                String dest_account = ((TransferOp) op).getDestAccount();

                msg_op = DistLedgerCommonDefinitions.Operation.newBuilder()
                        .setType(op_type)
                        .setUserId(userId)
                        .setDestUserId(dest_account)
                        .setAmount(amount)
                        .setPrev(prev)
                        .setTS(TS)
                        .setStable(op.isStable())
                        .build();
            } else {
                op_type = OP_CREATE_ACCOUNT;

                msg_op = DistLedgerCommonDefinitions.Operation.newBuilder()
                        .setType(op_type)
                        .setUserId(userId)
                        .setPrev(prev)
                        .setTS(TS)
                        .setStable(op.isStable())
                        .build();
            }

            list.add(msg_op);
        }
        DistLedgerCommonDefinitions.LedgerState ls = DistLedgerCommonDefinitions.LedgerState.newBuilder().addAllLedger(list).build();

        PropagateStateRequest request= PropagateStateRequest.newBuilder().setState(ls).build();

        // chamar propagate state
        stub.propagateState(request);


    }

        //Function used in secondary Server to receive propagated state
    public void propagateState(List<DistLedgerCommonDefinitions.Operation> ops, CrossServerDistLedger.VectorClock senderReplicaTS) {

        List<Operation> opsLst = new ArrayList<>();

        for (DistLedgerCommonDefinitions.Operation op : ops) {

            Operation opObj = null;
            DistLedgerCommonDefinitions.OperationType op_type;
            String userId = op.getUserId();
            VectorClock prev = new VectorClock(new ArrayList<>(op.getPrev().getTsList()));
            VectorClock TS = new VectorClock(new ArrayList<>(op.getTS().getTsList()));
            boolean stable = op.getStable();
            op_type = op.getType();

            if (op_type == OP_TRANSFER_TO) {
                opObj = new TransferOp(userId, op.getDestUserId(), op.getAmount(), prev, TS, stable, "OP_TRANSFER_TO");
            } else if (op_type == OP_CREATE_ACCOUNT) {
                opObj = new CreateOp(userId, prev, TS, stable, "OP_CREATE_ACCOUNT");
            }

            if (opObj != null) {
                opsLst.add(opObj);
            }
        }

        for (Operation opObj : opsLst) {
            if (opObj.getTS().GE(this.replicaTS)) {

                addLedgerEntry(opObj);
                addToPropagateBuffer(opObj);

                if (this.valueTS.GE(opObj.getPrev())) {
                    opObj.setStable(true);

                    // execute op
                    if (opObj instanceof TransferOp) {
                        int amount = ((TransferOp) opObj).getAmount();
                        String dest_account = ((TransferOp) opObj).getDestAccount();
                        accounts.put(opObj.getAccount(), getBalance(opObj.getAccount()) - amount);
                        accounts.put(dest_account, getBalance(dest_account) + amount);
                    } else if (opObj instanceof CreateOp) {
                        addAccount(opObj.getAccount());
                    }

                    mergeTS(this.valueTS, opObj.getTS());
                }
            }
        }

        // step 2
        mergeTS(this.replicaTS, new VectorClock(new ArrayList<>(senderReplicaTS.getTsList())));

        // step 3
        for (Operation op : this.ledger) {
            if (this.valueTS.GE(op.getPrev()) && !op.isStable()) {
                op.setStable(true);
                // execute op
                mergeTS(this.valueTS, op.getTS());
            }
        }
    }


    //function used in primary server to add operations to a buffer and when buffer is full send them to
    public void addToPropagateBuffer(Operation operation) throws RuntimeException {
        if (propagateBuffer.size() >= 4) {
            propagateBuffer.add(operation);
            List<DistLedgerCommonDefinitions.Operation> list = new ArrayList<DistLedgerCommonDefinitions.Operation>();
            for (pt.tecnico.distledger.server.domain.operation.Operation op : propagateBuffer) {

                DistLedgerCommonDefinitions.VectorClock prev = DistLedgerCommonDefinitions.VectorClock.newBuilder()
                        .addAllTs(op.getPrev().getFullTs())
                        .build();

                DistLedgerCommonDefinitions.VectorClock TS = DistLedgerCommonDefinitions.VectorClock.newBuilder()
                        .addAllTs(op.getTS().getFullTs())
                        .build();

                DistLedgerCommonDefinitions.OperationType op_type;
                String userId = op.getAccount();
                DistLedgerCommonDefinitions.Operation msg_op;
                if (op instanceof TransferOp) {
                    op_type = OP_TRANSFER_TO;
                    int amount = ((TransferOp) op).getAmount();
                    String dest_account = ((TransferOp) op).getDestAccount();

                    msg_op = DistLedgerCommonDefinitions.Operation.newBuilder()
                            .setType(op_type)
                            .setUserId(userId)
                            .setDestUserId(dest_account)
                            .setAmount(amount)
                            .setPrev(prev)
                            .setTS(TS)
                            .setStable(op.isStable())
                            .build();
                } else {
                    op_type = OP_CREATE_ACCOUNT;

                    msg_op = DistLedgerCommonDefinitions.Operation.newBuilder()
                            .setType(op_type)
                            .setUserId(userId)
                            .setPrev(prev)
                            .setTS(TS)
                            .setStable(op.isStable())
                            .build();
                }

                list.add(msg_op);
            }
            DistLedgerCommonDefinitions.LedgerState ls = DistLedgerCommonDefinitions.LedgerState.newBuilder().addAllLedger(list).build();

            //create request and send with stub

            if (stub == null) {

                ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
                DistLedgerCrossServerServiceGrpc.DistLedgerCrossServerServiceBlockingStub lookUpStub = DistLedgerCrossServerServiceGrpc.newBlockingStub(channel);
                LookupRequest lookupRequest = LookupRequest.newBuilder().setServiceName(serviceName).build();
                LookupResponse response = lookUpStub.lookup(lookupRequest);
                List<NamingServerEntry> entries = response.getServerEntriesList();
                String serverAddress = null;
                for (NamingServerEntry nse : entries) {
                    if (nse.getServerQualifier().compareTo(secondaryQualifier) == 0) {
                        serverAddress = nse.getServerAddress();

                    }
                }
                System.out.println(serverAddress);
                createStub(serverAddress);

            }
            PropagateStateRequest request= PropagateStateRequest.newBuilder().setState(ls).build();
            try {
                stub.withDeadlineAfter(deadlineMs, TimeUnit.MILLISECONDS).propagateState(request);
            }
            catch (StatusRuntimeException e) {
                stub = null;
                throw new StatusRuntimeException(Status.DEADLINE_EXCEEDED.withDescription("Server request time out."));
            }
            propagateBuffer.clear();
        }
        else {
            propagateBuffer.add(operation);
        }
    }

    public void mergeTS(VectorClock v1, VectorClock v2) {
        for (int i = 0; i < v1.getFullTs().size(); i++) {
            if (v1.getTS(i) < v2.getTS(i)) {
                v1.setTS(i, v2.getTS(i));
            }
        }
    }

    public VectorClock writeRequest(VectorClock prev) {

        // update replicaTS
        this.replicaTS.setTS(this.id, replicaTS.getTS(this.id) + 1);

        // create operationTS -> return
        VectorClock opTS = new VectorClock();
        for (int i = 0; i < prev.getFullTs().size(); i++) {
            opTS.setTS(i, prev.getTS(i));
        }
        opTS.setTS(this.id, this.replicaTS.getTS(this.id));

        return opTS;
    }
}
