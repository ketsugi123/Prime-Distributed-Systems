### ISEL - Computação Distribuída
##### Inverno 2024/25
----
### Trabalho Prático de Avaliação (TPA1)

Grupo: **G16**

Membros:
- 49457 - Alexandre Severino
- 49473 - Diogo Carichas
  
## Guia de Instalação
 
### Requisitos
- Java
- Maven

### 1. Ring Manager
1. Fazer pull da imagem de Docker
    ```
    docker pull cdg16/ringmanager
    ```
2. Criar o contentor Ring Manager
    ```
    docker run -p <RING_MANAGER_PORT>:<RING_MANAGER_PORT> -d cdg16/ringmanager <RING_MANAGER_PORT>
    ```

### 2. Prime Server
1. Fazer pull da imagem de Docker
    ```
    docker pull cdg16/primeserver
    docker pull cdg16/isprime
    docker pull redis
    ```
2. Criar o contentor de Prime Server, especificando a sua porta, a porta de base de dados REDIS e o endereço externo do Ring Manager
    ```
    docker run -p <PRIME_SERVER_PORT>:<PRIME_SERVER_PORT> -d -v /var/run/docker.sock:/var/run/docker.sock cdg16/primeserver <PRIME_SERVER_ADDRESS> <PRIME_SERVER_PORT> <REDIS_PORT> <RING_MANAGER_ADDRESS> <RING_MANAGER_PORT>
    ```

### 3.  Cliente

-  Executar os seguintes comandos:
    ```
    mvn package
    java -jar target/grpcClient-1.0-jar-with-dependencies.jar 8000 <RING_MANAGER_ADDRESS>
    ```
