package pt.tecnico.distledger.server;

import io.grpc.*;
import io.grpc.Server;
import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.userclient.grpc.UserService;
import pt.tecnico.distledger.adminclient.grpc.AdminService;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.DistLedgerCrossServerServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.CrossServerDistLedger.*;

import java.io.IOException;

public class ServerMain {

    private static ServerState serverState = new ServerState();

    private static final boolean DEBUG_FLAG = (System.getProperty("debug") != null);
    public static void main(String[] args) throws IOException, InterruptedException{
        System.out.println(ServerMain.class.getSimpleName());

        // receive and print arguments
        System.out.printf("Received %d arguments%n", args.length);
        for (int i = 0; i < args.length; i++) {
            System.out.printf("arg[%d] = %s%n", i, args[i]);
        }

        // check arguments
        if (args.length < 1) {
            System.err.println("Argument(s) missing!");
            System.err.printf("Usage: java %s port%n", ServerMain.class.getName());
            return;
        }

        if(DEBUG_FLAG){
            serverState.setDebug(true);
        }

        if(args[1].compareTo("A")==0){
            serverState.setPrimary(true);
        }
        //creating stub
        ManagedChannel channel = ManagedChannelBuilder.forTarget("localhost:5001").usePlaintext().build();
        DistLedgerCrossServerServiceGrpc.DistLedgerCrossServerServiceBlockingStub stub = DistLedgerCrossServerServiceGrpc.newBlockingStub(channel);
        RegisterRequest request = RegisterRequest.newBuilder().setServerAddress("localhost"+":"+args[0])
                .setServerQualifier(args[1]).setServiceName("DistLedger").build();

        stub.register(request);
        final int port = Integer.parseInt(args[0]);
        final BindableService adminService = new AdminServiceImpl(serverState);
        final BindableService userService = new UserServiceImpl(serverState);
        // Create a new server to listen on port
        Server server;
        if(serverState.isPrimary()){
            server = ServerBuilder.forPort(port).addService(adminService).addService(userService).build();
        }else{
            final BindableService secondaryServerService = new SecondaryServerServiceImpl(serverState);
            server = ServerBuilder.forPort(port).addService(adminService).addService(userService).addService(secondaryServerService).build();
        }

        // Start the server
        server.start();

        // Server threads are running in the background.
        System.out.println("Server started");

        // Do not exit the main thread. Wait until server is terminated.
        server.awaitTermination();
        /* TODO */
    }

}

