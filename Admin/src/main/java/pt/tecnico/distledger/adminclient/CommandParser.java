package pt.tecnico.distledger.adminclient;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import pt.tecnico.distledger.adminclient.grpc.AdminService;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.*;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger;

import java.util.Scanner;

public class CommandParser {

    private static final String SPACE = " ";
    private static final String ACTIVATE = "activate";
    private static final String DEACTIVATE = "deactivate";
    private static final String GET_LEDGER_STATE = "getLedgerState";
    private static final String GOSSIP = "gossip";
    private static final String HELP = "help";
    private static final String EXIT = "exit";

    private final AdminService adminService;
    public CommandParser(AdminService adminService) {
        this.adminService = adminService;
    }
    void parseInput() {

        Scanner scanner = new Scanner(System.in);
        boolean exit = false;

        while (!exit) {
            System.out.print("> ");
            String line = scanner.nextLine().trim();
            String cmd = line.split(SPACE)[0];

            switch (cmd) {
                case ACTIVATE:
                    this.activate(line);
                    break;

                case DEACTIVATE:
                    this.deactivate(line);
                    break;

                case GET_LEDGER_STATE:
                    this.dump(line);
                    break;

                case GOSSIP:
                    this.gossip(line);
                    break;

                case HELP:
                    this.printUsage();
                    break;

                case EXIT:
                    exit = true;
                    break;

                default:
                    break;
            }

        }
    }

    private void activate(String line){
        String[] split = line.split(SPACE);
        int num_tries = 3;

        if (split.length != 2){
            this.printUsage();
            return;
        }

        String server = split[1];

        if (adminService.getQualifier().compareTo(server) != 0) {
            adminService.lookup(server);
        }

        while (num_tries > 0) {
            try {
                ActivateRequest request = ActivateRequest.newBuilder()
                        .build();

                ActivateResponse response = adminService.activate(request);
                System.out.println("OK");
                break;
            }
            catch (StatusRuntimeException e) {
                if (Status.DEADLINE_EXCEEDED.getCode().equals(e.getStatus().getCode())) {
                    adminService.lookup(server);
                    num_tries--;
                }
                else {
                    System.out.println(e.getStatus().getDescription());
                    break;
                }
            }
        }
        if (num_tries == 0) {
            System.out.println("No servers available to process your request.");
        }
    }

    private void deactivate(String line){
        String[] split = line.split(SPACE);
        int num_tries = 3;

        if (split.length != 2){
            this.printUsage();
            return;
        }

        String server = split[1];

        if (adminService.getQualifier().compareTo(server) != 0) {
            adminService.lookup(server);
        }

        while (num_tries > 0) {
            try {
                DeactivateRequest request = DeactivateRequest.newBuilder()
                        .build();

                DeactivateResponse response = adminService.deactivate(request);
                System.out.println("OK");
                break;
            }
            catch (StatusRuntimeException e) {
                if (Status.DEADLINE_EXCEEDED.getCode().equals(e.getStatus().getCode())) {
                    adminService.lookup(server);
                    num_tries--;
                }
                else {
                    System.out.println(e.getStatus().getDescription());
                    break;
                }
            }
        }
        if (num_tries == 0) {
            System.out.println("No servers available to process your request.");
        }
    }

    private void dump(String line){
        String[] split = line.split(SPACE);
        int num_tries = 3;

        if (split.length != 2){
            this.printUsage();
            return;
        }
        String server = split[1];

        if (adminService.getQualifier().compareTo(server) != 0) {
            adminService.lookup(server);
        }

        while (num_tries > 0) {
            try {
                getLedgerStateRequest request = getLedgerStateRequest.newBuilder()
                        .build();

                getLedgerStateResponse response = adminService.getLedgerState(request);
                System.out.println("OK");
                System.out.print(response);
                break;
            }
            catch (StatusRuntimeException e) {
                if (Status.DEADLINE_EXCEEDED.getCode().equals(e.getStatus().getCode())) {
                    adminService.lookup(server);
                    num_tries--;
                }
                else {
                    System.out.println(e.getStatus().getDescription());
                    break;
                }
            }
        }
        if (num_tries == 0) {
            System.out.println("No servers available to process your request.");
        }
    }

    //@SuppressWarnings("unused")
    private void gossip(String line){
        String[] split = line.split(SPACE);

        String server = split[1];

        if (adminService.getQualifier().compareTo(server) != 0) {
            adminService.lookup(server);
        }


        try {
                GossipRequest request = GossipRequest.newBuilder()
                        .build();

                GossipResponse response = adminService.gossip(request);
                System.out.println("OK");

        }
        catch (StatusRuntimeException e) {
            System.out.println(e.getStatus().getDescription());
        }


    }
    private void printUsage() {
        System.out.println("Usage:\n" +
                "- activate <server>\n" +
                "- deactivate <server>\n" +
                "- getLedgerState <server>\n" +
                "- gossip <server>\n" +
                "- exit\n");
    }

}
