package pt.tecnico.distledger.adminclient.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.*;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.CrossServerDistLedger;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.DistLedgerCrossServerServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.*;
import pt.ulisboa.tecnico.distledger.contract.user.UserServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.CrossServerDistLedger.*;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class AdminService {

    final String ns_target = "localhost:5001";

    String target;

    String qualifier;

    final String primary_qualifier = "A";

    final String service = "DistLedger";

    final int deadlineMs = 2000;
    final int deadlineMsLong = 4000;

    private final DistLedgerCrossServerServiceGrpc.DistLedgerCrossServerServiceBlockingStub ns_stub;

    private AdminServiceGrpc.AdminServiceBlockingStub stub;

    public AdminService() {
        ManagedChannel ns_channel = ManagedChannelBuilder.forTarget(ns_target).usePlaintext().build();
        ns_stub = DistLedgerCrossServerServiceGrpc.newBlockingStub(ns_channel);
        lookup();
    }

    /*
    public AdminService(String target) {
        this.target = target;
        buildChannelAndStub(this.target);
    }
    */

    private void buildChannelAndStub(String target) {
        ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
        this.stub = AdminServiceGrpc.newBlockingStub(channel);
    }


    public String getQualifier() {
        return qualifier;
    }

    private void GetServerWithQualifier(CrossServerDistLedger.LookupResponse response, String new_qualifier) {
        Optional<CrossServerDistLedger.NamingServerEntry> new_server = response.getServerEntriesList()
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

    private void lookup() {
        try {
            CrossServerDistLedger.LookupRequest request = CrossServerDistLedger.LookupRequest.newBuilder()
                    .setServiceName(service)
                    .build();

            CrossServerDistLedger.LookupResponse response = ns_stub.lookup(request);
            GetServerWithQualifier(response, primary_qualifier);

        }
        catch (StatusRuntimeException e) {
            System.out.println(e.getStatus().getDescription());
        }
    }

    public void lookup(String qualifier) {
        try {
            CrossServerDistLedger.LookupRequest request = CrossServerDistLedger.LookupRequest.newBuilder()
                    .setServiceName(service)
                    .setQualifier(qualifier)
                    .build();

            CrossServerDistLedger.LookupResponse response = ns_stub.lookup(request);
            GetServerWithQualifier(response, qualifier);

        }
        catch (StatusRuntimeException e) {
            System.out.println(e.getStatus().getDescription());
        }
    }

    public ActivateResponse activate(ActivateRequest request) {
        return stub.withDeadlineAfter(deadlineMs, TimeUnit.MILLISECONDS).activate(request);
    }

    public DeactivateResponse deactivate(DeactivateRequest request) {
        return stub.withDeadlineAfter(deadlineMs, TimeUnit.MILLISECONDS).deactivate(request);
    }

    public getLedgerStateResponse getLedgerState(getLedgerStateRequest request) {
        return stub.withDeadlineAfter(deadlineMsLong, TimeUnit.MILLISECONDS).getLedgerState(request);
    }

    public GossipResponse gossip(GossipRequest request){
        return stub.gossip(request);
    }
}
