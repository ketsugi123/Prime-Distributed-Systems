# Prime Distributed Systems

The goal is to develop a distributed system (Prime Distributed Systems) where multiple servers are used 
to support load distribution across multiple requests from clients, 
as shown in Figure 1. A request consists of checking whether a long integer is a prime number or not.

## Tools used
1. **Docker**: To containerize all components of the system;
2. **Java 11**: Java version used in this project;
3. **Maven**: Dependency manager for the project.
## Components and Contracts

### 1. **RingManager Server**
The RingManager server has a well-known location (IP, port) and provides two contracts:

1. **Register Servers (1.registServer):**
    - Used by the servers (PrimeServers) to register by indicating their IP and port.
    - Allows servers to obtain the IP and port of the next server in the ring.

2. **Get PrimeServer (3.getPrimeServer):**
    - A contract for clients to get the IP and port of a server to which they can submit requests.

### 2. **PrimeServer**
Each PrimeServer provides two contracts:

1. **Check Prime (4.isPrime(N)):**
    - A contract to receive client requests and check whether a number is prime.

2. **Ring Message (5.ringMessage()):**
    - A contract to receive requests from the preceding server in the ring.

### 3. **Client Application**
The client application uses two contracts:

1. **Access to RingManager (Get PrimeServer):**
    - Allows the client to access the RingManager and obtain the location of a registered server.

2. **Prime Check (isPrime):**
    - Allows the client to query a server to check if a given number is prime.

## System Behavior

- **Load Balancing:**
    - The RingManager server must distribute clients evenly across the available servers in the ring.

- **Prime Check Process:**
    - When a PrimeServer receives a request from a client to check if a number is prime, it consults a dictionary (key, value) stored in a Redis server (running in a Docker container).
        - If the dictionary contains information about the number, the server responds immediately to the client.
        - If the dictionary does not contain information, the server forwards the request to the next server in the ring.
        - As the message travels around the ring, each server can update the message with information it knows (whether the number is prime or not). If any server has the information, it updates its local Redis dictionary.

- **Message Flow in the Ring:**
    - If the message comes back to the initial server with the information, it will respond to the client.
    - If no information is available, the server will launch a Docker container to process the primality of the number.

## Docker Containers and API Usage

- **Docker Containers:**
    - A Docker container may be launched dynamically to process whether a number is prime using the provided API.

- **Client Response:**
    - The client always receives a response (either true or false), independent of the interactions between the PrimeServers.

## Functional Requirements

- **Loose Coupling:**
    - There should be four contracts:
        1. **PrimeServer to RingManager** for server registration.
        2. **Client to RingManager** to obtain the endpoint (IP, port) of a PrimeServer.
        3. **Client to PrimeServer** to submit requests.
        4. **Message Forwarding between PrimeServers** within the ring.

- **Server Ring:**
    - The PrimeServers should be arranged in a ring. Each server forwards the message to the next one until it finds or processes the result.

- **Fault Tolerance:**
    - The RingManager server is assumed to never fail and is located at a well-known IP and port.

## Deployment and Scalability

- **Multiple Virtual Machines (VMs):**
    - Each VM can host one or more PrimeServers.
    - The prototype must have at least three PrimeServers running across at least two VMs, with at least one client application connected to each server.

- **Client Deployment:**
    - Client instances can run on personal machines or on the VMs hosting the servers.

- **Dynamic Server Addition:**
    - The system should support adding new servers dynamically, improving load balancing.



