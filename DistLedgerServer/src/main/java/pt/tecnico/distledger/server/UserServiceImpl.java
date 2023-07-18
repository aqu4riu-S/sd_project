package pt.tecnico.distledger.server;

import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import pt.tecnico.distledger.server.domain.ServerState;
import pt.ulisboa.tecnico.distledger.contract.user.UserServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.*;

import java.util.List;

public class UserServiceImpl extends UserServiceGrpc.UserServiceImplBase{

    private final ServerState server_state;

    public UserServiceImpl(ServerState server_state) {
        this.server_state = server_state;
    }

    private pt.tecnico.distledger.server.domain.VectorClock processVectorClock(VectorClock v) {
        pt.tecnico.distledger.server.domain.VectorClock vec = new pt.tecnico.distledger.server.domain.VectorClock();
        List<Integer> timeStamps = v.getTsList();
        for (int i = 0; i < timeStamps.size(); i++) {
            vec.setTS(i, timeStamps.get(i));
        }
        return vec;
    }

    @Override
    public void createAccount(CreateAccountRequest request, StreamObserver<CreateAccountResponse> responseObserver) {
        try {
            VectorClock v = request.getPrev();

            pt.tecnico.distledger.server.domain.VectorClock vec = processVectorClock(v);

            server_state.createAccount(request.getUserId(), vec);

            VectorClock newV = VectorClock.newBuilder()
                    .addAllTs(server_state.getValueTS().getFullTs())
                    .build();

            CreateAccountResponse response = CreateAccountResponse.newBuilder().setNew(newV).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
        catch (StatusRuntimeException e) {
            responseObserver.onError(e);
        }
    }

    @Override
    public void balance(BalanceRequest request, StreamObserver<BalanceResponse> responseObserver) {
        try {
            VectorClock v = request.getPrev();

            pt.tecnico.distledger.server.domain.VectorClock vec = processVectorClock(v);

            int balance = server_state.balance(request.getUserId(), vec);

            VectorClock newV = VectorClock.newBuilder()
                    .addAllTs(server_state.getValueTS().getFullTs())
                    .build();

            BalanceResponse response = BalanceResponse.newBuilder()
                    .setValue(balance)
                    .setNew(newV)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
        catch (StatusRuntimeException e) {
            responseObserver.onError(e);
        }
    }

    @Override
    public void transferTo(TransferToRequest request, StreamObserver<TransferToResponse> responseObserver) {
        try {
            VectorClock v = request.getPrev();
            pt.tecnico.distledger.server.domain.VectorClock vec = processVectorClock(v);

            server_state.transferTo(
                    request.getAccountFrom(),
                    request.getAccountTo(),
                    request.getAmount(),
                    vec
            );

            VectorClock newV = VectorClock.newBuilder()
                    .addAllTs(server_state.getValueTS().getFullTs())
                    .build();

            TransferToResponse response = TransferToResponse.newBuilder().setNew(newV).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
        catch (StatusRuntimeException e) {
            responseObserver.onError(e);
        }
    }
}
