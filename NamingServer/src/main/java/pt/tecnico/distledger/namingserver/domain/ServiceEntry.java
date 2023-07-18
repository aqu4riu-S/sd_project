package pt.tecnico.distledger.namingserver.domain;

import java.util.ArrayList;
import java.util.List;

public class ServiceEntry {
    String ServiceName;
    List<ServerEntry> serverEntries;

    public ServiceEntry(String ServiceName){
        this.ServiceName = ServiceName;
        this.serverEntries = new ArrayList<ServerEntry>();
    }

    public String getServiceName() {
        return ServiceName;
    }

    public void setServiceName(String serviceName) {
        ServiceName = serviceName;
    }

    public List<ServerEntry> getServerEntries() {
        return serverEntries;
    }
    public void addServerEntry(ServerEntry entry){
        serverEntries.add(entry);
    }

    public void removeServerEntry(ServerEntry entry){
        serverEntries.remove(entry);//maybe need to check if the element is in the list
    }

}