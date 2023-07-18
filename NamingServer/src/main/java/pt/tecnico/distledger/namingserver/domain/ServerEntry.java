package pt.tecnico.distledger.namingserver.domain;

public class ServerEntry {
    String ServerAddress;
    String ServerQualifier;

    public ServerEntry(String ServerAddress,String ServerQualifier){
        this.ServerAddress=ServerAddress;
        this.ServerQualifier = ServerQualifier;
    }

    public String getServerAddress() {
        return ServerAddress;
    }

    public void setServerAddress(String serverAddress) {
        ServerAddress = serverAddress;
    }

    public String getServerQualifier() {
        return ServerQualifier;
    }

    public void setServerQualifier(String serverQualifier) {
        ServerQualifier = serverQualifier;
    }
}