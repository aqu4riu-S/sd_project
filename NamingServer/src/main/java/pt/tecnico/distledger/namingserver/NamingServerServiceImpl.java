package pt.tecnico.distledger.namingserver;


import pt.ulisboa.tecnico.distledger.contract.distledgerserver.DistLedgerCrossServerServiceGrpc;
import pt.tecnico.distledger.namingserver.domain.NamingServerState;
import java.util.List;
import java.util.ArrayList;
import pt.tecnico.distledger.namingserver.domain.ServerEntry;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.CrossServerDistLedger.*;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions.*;
public class NamingServerServiceImpl extends DistLedgerCrossServerServiceGrpc.DistLedgerCrossServerServiceImplBase{

    private final NamingServerState namingServer;

    public NamingServerServiceImpl(NamingServerState namingServer) {
        this.namingServer = namingServer;
    }

    @Override
    public void register(RegisterRequest request, StreamObserver<RegisterResponse> responseObserver) {
        try {
            namingServer.register(request.getServiceName(),request.getServerQualifier(),request.getServerAddress());
            responseObserver.onNext(RegisterResponse.newBuilder().build());
            responseObserver.onCompleted();
        }
        catch (StatusRuntimeException e) {
            responseObserver.onError(e);
        }
    }

    @Override
    public void lookup(LookupRequest request, StreamObserver<LookupResponse> responseObserver) {
        try {
            List<ServerEntry> entries =namingServer.lookup(request.getServiceName());
            List<NamingServerEntry>messages= new ArrayList<NamingServerEntry>();
            for(ServerEntry entry:entries){
                messages.add( NamingServerEntry.newBuilder().setServerQualifier(entry.getServerQualifier()).setServerAddress(entry.getServerAddress()).build());
            }
            responseObserver.onNext(LookupResponse.newBuilder().addAllServerEntries(messages).build());
            responseObserver.onCompleted();
        }
        catch (StatusRuntimeException e) {
            responseObserver.onError(e);
        }
    }
}