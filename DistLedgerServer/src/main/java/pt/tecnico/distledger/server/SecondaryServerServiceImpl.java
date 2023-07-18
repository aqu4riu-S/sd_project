package pt.tecnico.distledger.server;

import pt.ulisboa.tecnico.distledger.contract.distledgerserver.DistLedgerCrossServerServiceGrpc;
import java.util.List;
import java.util.ArrayList;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.CrossServerDistLedger.*;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions.*;
import pt.tecnico.distledger.server.domain.ServerState;

public class SecondaryServerServiceImpl extends DistLedgerCrossServerServiceGrpc.DistLedgerCrossServerServiceImplBase{

    private final ServerState serverState;

    public SecondaryServerServiceImpl(ServerState serverState) {
        this.serverState = serverState;
    }
    @Override
    public void propagateState(PropagateStateRequest request, StreamObserver<PropagateStateResponse> responseObserver){
        try {
            pt.ulisboa.tecnico.distledger.contract.distledgerserver.CrossServerDistLedger.VectorClock v = request.getReplicaTS();
            LedgerState state = request.getState();
            List<Operation>ops =state.getLedgerList();
            serverState.propagateState(ops, v);
            responseObserver.onNext(PropagateStateResponse.newBuilder().build());
            responseObserver.onCompleted();
        }
        catch (StatusRuntimeException e) {
            responseObserver.onError(e);
        }
    }

    @Override
    public void isAvailable(IsAvailableRequest request, StreamObserver<IsAvailableResponse> responseObserver) {
        try {
            IsAvailableResponse response = IsAvailableResponse.newBuilder()
                    .setIsAvailable(serverState.isAvailable())
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
        catch (StatusRuntimeException e) {
            responseObserver.onError(e);
        }
    }

}