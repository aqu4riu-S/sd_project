# DistLedger

Distributed Systems Project 2022/2023

## Authors

**Group A57**

### Code Identification

In all source files (namely in the *groupId*s of the POMs), replace __GXX__ with your group identifier. The group
identifier consists of either A or T followed by the group number - always two digits. This change is important for 
code dependency management, to ensure your code runs using the correct components and not someone else's.

### Team Members


| Number | Name              | User                                         | Email                                             |
|--------|-------------------|----------------------------------------------|---------------------------------------------------|
| 99187  | Bruno Campos      | <https://github.com/deft-24>                 | <mailto:brunovcampos@tecnico.ulisboa.pt>          |
| 99227  | Gonçalo Carvalho  | <https://github.com/goncalo-jp-carvalho>     | <mailto:goncalo.pires.carvalho@tecnico.ulisboa.pt>|
| 99295  | Orlando Dutra     | <https://github.com/ist199295>               | <mailto:orlandomelodutra@tecnico.ulisboa.pt>      |

## Getting Started

The overall system is made up of several modules. The main server is the _DistLedgerServer_. The clients are the _User_ 
and the _Admin_. The definition of messages and services is in the _Contract_. The future naming server
is the _NamingServer_.

See the [Project Statement](https://github.com/tecnico-distsys/DistLedger) for a complete domain and system description.

### Prerequisites

The Project is configured with Java 17 (which is only compatible with Maven >= 3.8), but if you want to use Java 11 you
can too -- just downgrade the version in the POMs.

To confirm that you have them installed and which versions they are, run in the terminal:

```s
javac -version
mvn -version
```

### Installation and Execution

To compile and install all modules:

```s
mvn clean install
```

Then the NamingServer should be started by issuing the following commands:

```s
cd NamingServer
mvn exec:java
```

After starting the NamingServer, 2 servers can be started by issuing the commands that follow:

```s
cd DistLedgerServer;
mvn exec:java -Dexec.args="2001 A"
mvn exec:java -Dexec.args="2002 B"
```

And finally client instances can be launched by changing directories into User or Admin Client and starting it, as follows:

```s
cd User 
cd Admin
mvn exec:java
```



## Built With

* [Maven](https://maven.apache.org/) - Build and dependency management tool;
* [gRPC](https://grpc.io/) - RPC framework.
