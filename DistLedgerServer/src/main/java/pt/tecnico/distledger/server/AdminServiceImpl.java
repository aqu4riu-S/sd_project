package pt.tecnico.distledger.server;

import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.domain.operation.DeleteOp;
import pt.tecnico.distledger.server.domain.operation.TransferOp;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.*;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions.*;

import java.util.ArrayList;
import java.util.List;

import static pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions.OperationType.*;

public class AdminServiceImpl extends AdminServiceGrpc.AdminServiceImplBase{

    private final ServerState server_state;

    public AdminServiceImpl(ServerState server_state) {
        this.server_state = server_state;
    }

    private pt.tecnico.distledger.server.domain.VectorClock processVectorClock(pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.VectorClock v) {
        pt.tecnico.distledger.server.domain.VectorClock vec = new pt.tecnico.distledger.server.domain.VectorClock();
        List<Integer> timeStamps = v.getTsList();
        for (int i = 0; i < timeStamps.size(); i++) {
            vec.setTS(i, timeStamps.get(i));
        }
        return vec;
    }

    @Override
    public void activate(ActivateRequest request, StreamObserver<ActivateResponse> responseObserver) {
        try {
            server_state.activate();
            //ActivateResponse response = ActivateResponse.newBuilder().build();
            responseObserver.onNext(ActivateResponse.newBuilder().build());
            responseObserver.onCompleted();
        }
        catch (StatusRuntimeException e) {
            responseObserver.onError(e);
        }
    }

    @Override
    public void deactivate(DeactivateRequest request, StreamObserver<DeactivateResponse> responseObserver) {
        try {
            server_state.deactivate();
            //DeactivateResponse response = DeactivateResponse.newBuilder().build();
            responseObserver.onNext(DeactivateResponse.newBuilder().build());
            responseObserver.onCompleted();
        }
        catch (StatusRuntimeException e) {
            responseObserver.onError(e);
        }
    }

    @Override
    public void getLedgerState(getLedgerStateRequest request, StreamObserver<getLedgerStateResponse> responseObserver) {
        try {
            List<Operation> list = new ArrayList<>();

            pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.VectorClock v = request.getPrev();

            pt.tecnico.distledger.server.domain.VectorClock vec = processVectorClock(v);

            for (pt.tecnico.distledger.server.domain.operation.Operation op : server_state.getLedgerState(vec)) {

                pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions.VectorClock prev = pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions.VectorClock.newBuilder()
                        .addAllTs(op.getPrev().getFullTs())
                        .build();

                pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions.VectorClock TS = pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions.VectorClock.newBuilder()
                        .addAllTs(op.getTS().getFullTs())
                        .build();

                OperationType op_type;
                String userId = op.getAccount();
                Operation operation;
                if (op instanceof TransferOp) {
                    op_type = OP_TRANSFER_TO;
                    int amount = ((TransferOp) op).getAmount();
                    String dest_account = ((TransferOp) op).getDestAccount();

                    operation = Operation.newBuilder()
                            .setType(op_type)
                            .setUserId(userId)
                            .setDestUserId(dest_account)
                            .setAmount(amount)
                            .setPrev(prev)
                            .setTS(TS)
                            .setStable(op.isStable())
                            .build();
                }
                else {
                    op_type = op instanceof DeleteOp ? OP_DELETE_ACCOUNT : OP_CREATE_ACCOUNT;

                    operation = Operation.newBuilder()
                            .setType(op_type)
                            .setUserId(userId)
                            .setPrev(prev)
                            .setTS(TS)
                            .setStable(op.isStable())
                            .build();
                }

                list.add(operation);
            }

            pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.VectorClock newV = pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.VectorClock.newBuilder()
                    .addAllTs(server_state.getValueTS().getFullTs())
                    .build();

            LedgerState ls = LedgerState.newBuilder().addAllLedger(list).build();
            getLedgerStateResponse lsr = getLedgerStateResponse.newBuilder()
                    .setLedgerState(ls)
                    .setNew(newV)
                    .build();

            responseObserver.onNext(lsr);
            responseObserver.onCompleted();
        }
        catch (StatusRuntimeException e) {
            responseObserver.onError(e);
        }
    }

    @Override
    public void gossip(GossipRequest request, StreamObserver<GossipResponse> responseObserver) {
        try {
            server_state.gossip();
            //DeactivateResponse response = DeactivateResponse.newBuilder().build();
            responseObserver.onNext(GossipResponse.newBuilder().build());
            responseObserver.onCompleted();
        }
        catch (StatusRuntimeException e) {
            responseObserver.onError(e);
        }
    }
}