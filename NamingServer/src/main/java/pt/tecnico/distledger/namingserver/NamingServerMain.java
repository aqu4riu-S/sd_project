package pt.tecnico.distledger.namingserver;

import  pt.tecnico.distledger.namingserver.domain.NamingServerState;
import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.io.IOException;
public class NamingServerMain {

    private static NamingServerState namingServerState= new NamingServerState();
    public static void main(String[] args)throws IOException, InterruptedException {

        final int port = 5001;

        final BindableService namingServerService = new NamingServerServiceImpl(namingServerState);

        // Create a new server to listen on port
        Server server = ServerBuilder.forPort(port).addService(namingServerService).build();

        // Start the server
        server.start();

        // Server threads are running in the background.
        System.out.println("Server started");

        // Do not exit the main thread. Wait until server is terminated.
        server.awaitTermination();
        /* TODO */

    }

}
