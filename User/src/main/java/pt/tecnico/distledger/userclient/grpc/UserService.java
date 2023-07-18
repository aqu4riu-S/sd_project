package pt.tecnico.distledger.userclient.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Deadline;
import java.lang.RuntimeException;
import io.grpc.StatusRuntimeException;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.CrossServerDistLedger.LookupRequest;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.CrossServerDistLedger.LookupResponse;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.CrossServerDistLedger.NamingServerEntry;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.DistLedgerCrossServerServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.*;
import pt.ulisboa.tecnico.distledger.contract.user.UserServiceGrpc;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class UserService {

    String target;

    String qualifier;

    final String primary_qualifier = "A";

    final String service = "DistLedger";

    final int deadlineMs = 2000;

    private final DistLedgerCrossServerServiceGrpc.DistLedgerCrossServerServiceBlockingStub ns_stub;
    private UserServiceGrpc.UserServiceBlockingStub stub;

    public UserService(String ns_target) {
        ManagedChannel ns_channel = ManagedChannelBuilder.forTarget(ns_target).usePlaintext().build();
        ns_stub = DistLedgerCrossServerServiceGrpc.newBlockingStub(ns_channel);
        lookup(primary_qualifier);
    }

    public void buildChannelAndStub(String target) {
        ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
        this.stub = UserServiceGrpc.newBlockingStub(channel);
    }

    public String getQualifier() {
        return qualifier;
    }

    private void GetServerWithQualifier(LookupResponse response, String new_qualifier) {
        Optional<NamingServerEntry> new_server = response.getServerEntriesList()
                .stream()
                .filter(server -> server.getServerQualifier().compareTo(new_qualifier) == 0)
                .findFirst();
        if (new_server.isPresent()) {
            this.qualifier = new_server.get().getServerQualifier();
            this.target = new_server.get().getServerAddress();
            buildChannelAndStub(this.target);
        }
        // else exception
    }

    public void lookup(String qualifier) {
        try {
            LookupRequest request = LookupRequest.newBuilder()
                    .setServiceName(service)
                    .setQualifier(qualifier)
                    .build();

            LookupResponse response = ns_stub.lookup(request);
            GetServerWithQualifier(response, qualifier);

        }
        catch (StatusRuntimeException e) {
            System.out.println(e.getStatus().getDescription());
        }
    }

    public CreateAccountResponse createAccount(CreateAccountRequest request) {
        return stub.withDeadlineAfter(deadlineMs, TimeUnit.MILLISECONDS).createAccount(request);
    }

    public BalanceResponse balance(BalanceRequest request) {
        return stub.withDeadlineAfter(deadlineMs, TimeUnit.MILLISECONDS).balance(request);
    }

    public TransferToResponse transferTo(TransferToRequest request) {
        return stub.withDeadlineAfter(deadlineMs, TimeUnit.MILLISECONDS).transferTo(request);
    }
}
