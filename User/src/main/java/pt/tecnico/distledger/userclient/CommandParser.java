package pt.tecnico.distledger.userclient;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import pt.tecnico.distledger.userclient.grpc.UserService;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.*;
//import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

public class CommandParser {

    private static final String SPACE = " ";
    private static final String CREATE_ACCOUNT = "createAccount";
    private static final String DELETE_ACCOUNT = "deleteAccount";
    private static final String TRANSFER_TO = "transferTo";
    private static final String BALANCE = "balance";
    private static final String HELP = "help";
    private static final String EXIT = "exit";

    private final UserService userService;
    VectorClock prevTS;

    public CommandParser(UserService userService) {
        this.userService = userService;
        this.prevTS =  new VectorClock();
        this.prevTS.setTS(0,0);
        this.prevTS.setTS(1,0);
    }

    void parseInput() {

        Scanner scanner = new Scanner(System.in);
        boolean exit = false;

        while (!exit) {
            System.out.print("> ");
            String line = scanner.nextLine().trim();
            String cmd = line.split(SPACE)[0];

            try{
                switch (cmd) {
                    case CREATE_ACCOUNT:
                        this.createAccount(line);
                        break;

                    case TRANSFER_TO:
                        this.transferTo(line);
                        break;

                    case BALANCE:
                        this.balance(line);
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
            catch (Exception e){
                System.err.println(e.getMessage());
            }
        }
    }

    private void checkAndUpdate(List<Integer> ts){
        for(int i=0;i<2;i++){
            if(prevTS.getTS(i)<= ts.get(i))
                prevTS.setTS(i,ts.get(i));
        }
    }

    private void createAccount(String line){
        String[] split = line.split(SPACE);
        int num_tries = 3;

        if (split.length != 3){
            this.printUsage();
            return;
        }

        String server = split[1];
        String username = split[2];

        if (userService.getQualifier().compareTo(server) != 0) {
            userService.lookup(server);
        }

        while (num_tries > 0) {
            try {
                pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.VectorClock vec = pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.VectorClock.newBuilder().addAllTs(prevTS.getFullTs()).build();

                CreateAccountRequest request = CreateAccountRequest.newBuilder()
                        .setUserId(username)
                        .setPrev(vec)
                        .build();

                CreateAccountResponse response = userService.createAccount(request);

                checkAndUpdate(response.getNew().getTsList());

                System.out.println("OK");
                break;
            }
            catch (StatusRuntimeException e) {
                if (Status.DEADLINE_EXCEEDED.getCode().equals(e.getStatus().getCode())) {
                    userService.lookup(server);
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

    private void balance(String line){
        String[] split = line.split(SPACE);
        int num_tries = 3;

        if (split.length != 3){
            this.printUsage();
            return;
        }
        String server = split[1];
        String username = split[2];

        if (userService.getQualifier().compareTo(server) != 0) {
            userService.lookup(server);
        }

        while (num_tries > 0) {
            try {
                pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.VectorClock vec = pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.VectorClock.newBuilder().addAllTs(prevTS.getFullTs()).build();
                BalanceRequest request = BalanceRequest.newBuilder()
                        .setUserId(username)
                        .setPrev(vec)
                        .build();

                BalanceResponse response = userService.balance(request);

                checkAndUpdate(response.getNew().getTsList());

                int b = response.getValue();
                System.out.println("OK");
                if (b > 0) System.out.println(b);

                break;
            }
            catch (StatusRuntimeException e) {
                if (Status.DEADLINE_EXCEEDED.getCode().equals(e.getStatus().getCode())) {
                    userService.lookup(server);
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

    private void transferTo(String line){
        String[] split = line.split(SPACE);
        int num_tries = 3;

        if (split.length != 5){
            this.printUsage();
            return;
        }
        String server = split[1];
        String from = split[2];
        String dest = split[3];
        Integer amount = Integer.valueOf(split[4]);

        if (userService.getQualifier().compareTo(server) != 0) {
            userService.lookup(server);
        }

        while (num_tries > 0) {
            try {
                pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.VectorClock vec = pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.VectorClock.newBuilder().addAllTs(prevTS.getFullTs()).build();

                TransferToRequest request = TransferToRequest.newBuilder()
                        .setAccountFrom(from)
                        .setAccountTo(dest)
                        .setAmount(amount)
                        .setPrev(vec)
                        .build();

                TransferToResponse response = userService.transferTo(request);

                checkAndUpdate(response.getNew().getTsList());
                System.out.println("OK");
                break;
            }
            catch (StatusRuntimeException e) {
                if (Status.DEADLINE_EXCEEDED.getCode().equals(e.getStatus().getCode())) {
                    userService.lookup(server);
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

    private void printUsage() {
        System.out.println("Usage:\n" +
                "- createAccount <server> <username>\n" +
                "- balance <server> <username>\n" +
                "- transferTo <server> <username_from> <username_to> <amount>\n" +
                "- exit\n");
    }
}
