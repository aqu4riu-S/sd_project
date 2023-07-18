package pt.tecnico.distledger.userclient;


import io.grpc.Server;
import io.grpc.StatusRuntimeException;
import pt.tecnico.distledger.userclient.grpc.UserService;

import java.util.List;
import java.util.ArrayList;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger;
import pt.ulisboa.tecnico.distledger.contract.user.UserServiceGrpc;

public class UserClientMain {

    public static void main(String[] args) {

        final String host = "localhost";
        final String port = "5001";
        final String ns_target = host + ":" + port;

        CommandParser parser = new CommandParser(new UserService(ns_target));
        parser.parseInput();

        System.exit(0);

    }
}
