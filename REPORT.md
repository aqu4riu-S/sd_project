A implementação da arquitectura gossip envolveu a utilização de um conjunto de ferramentas estudadas no contexto desta 
cadeira. A componente de maior relevância para a concretização deste projeto foi a framework gRPC que permite a 
comunicação entre nós de uma rede. Para estabelecer uma ligação entre nós, utilizou-se o ManagedChannelBuilder que é 
responsável pela construção de um canal que permite estabelecer comunicação entre dois nós.

Uma vez estabelecida a ligação, utilizamos o serviço lookup para obter do NamingServer uma lista de servidores 
disponíveis. A resposta do servidor obtida através de um LookupResponse continha uma lista de objectos (NamingServerEntry). 
Iteramos então esta lista para encontrar uma entrada que correspondesse a um qualificador de uma réplica. Ao encontrar 
uma correspondência, o endereço do servidor é obtido através da NamingServerEntry e utilizado para criar um novo canal 
entre estes servidores.

Uma vez estabelecido um canal, construimos um PropagateStateRequest, composto por uma Ledger que corresponde ao estado 
do servidor e um VectorClock que corresponde à ReplicaTimestamp.

O admin é a entidade responsável pela invocação à função propagação passiva (em background) 
das modificações (estado) de uma réplica à(s) restante(s) - gossip.

Para isso, faz-se uso do serviço grpc gossip para a chamada ao método correspondente do server (réplica) escolhida
pelo admin para fazer essa propagação.

Neste ponto, essa réplica solicita ao servidor de nomes uma lista das réplicas que estão disponíveis e, depois, escolhe 
uma para fazer gossip (propagar o seu estado).

Do outro lado, a receção da propagação das modificações (estado da réplica) por parte da outra réplica é feita de acordo
com o algoritmo disponibilizado na aula de laboratório. 

Resumidamente, os pedidos são adicionados na ledger e são executados, caso a sua timeStamp não esteja atrasada face à
do pedido (operação). Depois, é percorrida a sua ledger para verificar se, dos pedidos já aí existentes, é possível
executar algum (porque entretanto a timeStamp da réplica mudou).

Globalmente, a implementação de uma arquitectura Gossip, no projecto, melhorou consideravelmente a funcionalidade 
e fiabilidade deste, permitindo uma partilha de dados e uma comunicação contínua entre os nós da rede.

Nota: optou-se por manter apenas uma estrutura para representar o estado de uma réplica (ledger), não tendo sida
implementada a tabela de execução de operações.