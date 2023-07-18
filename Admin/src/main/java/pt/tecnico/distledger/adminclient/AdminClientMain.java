package pt.tecnico.distledger.adminclient;

import pt.tecnico.distledger.adminclient.grpc.AdminService;

public class AdminClientMain {
    public static void main(String[] args) {

        CommandParser parser = new CommandParser(new AdminService());
        parser.parseInput();

        System.exit(0);

    }
}
