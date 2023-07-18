package pt.tecnico.distledger.namingserver.domain;

import pt.tecnico.distledger.namingserver.domain.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

public class NamingServerState {
    private Map<String,ServiceEntry> services;

    public NamingServerState(){

        this.services = new HashMap<String,ServiceEntry>();
    }

    public synchronized void register(String serviceName, String serverQualifier, String serverAddress){
        if(!services.containsKey(serviceName)){
            ServiceEntry entry = new ServiceEntry(serviceName);
            ServerEntry serverEntry = new ServerEntry(serverAddress,serverQualifier);
            entry.addServerEntry(serverEntry);
            services.put(serviceName,entry);
        }else{
            ServerEntry serverEntry = new ServerEntry(serverAddress,serverQualifier);
            services.get(serviceName).addServerEntry(serverEntry);
        }
    }

    public synchronized void delete(String serviceName, String serverAddress) throws RuntimeException{
        if(services.containsKey(serviceName)){
            for(ServerEntry entry:services.get(serviceName).getServerEntries()){
                if(entry.getServerAddress().compareTo(serverAddress)==0){
                    services.get(serviceName).removeServerEntry(entry);
                    return;
                }
            }
            throw new   StatusRuntimeException(Status.NOT_FOUND.withDescription("Not possible to remove the server"));
        }else{
          throw new   StatusRuntimeException(Status.NOT_FOUND.withDescription("Not possible to remove the server"));
        }
    }

    public List<ServerEntry> lookup(String serviceName){
        return services.get(serviceName).getServerEntries();

    }
}
