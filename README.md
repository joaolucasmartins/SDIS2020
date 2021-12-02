# Proj1 SDIS T5G08 - FEUP, 2021

## Group members

- João de Jesus Costa, up201806560@fe.up.pt
- João Lucas Silva Martins, up201806436@fe.up.pt

## Handout

1. Introduction

In this project you will develop a peer-to-peer distributed backup service for the Internet. The idea is to use the free disk space of the computers on the Internet for backing up files in one's own computer. As in the first project, the service must support the backup, restore and deletion of files. Also, the participants in the service must retain total control over their own storage, and therefore they may delete copies of files that they have previously stored.

2. Specification
   The design of the service is up to you. E.g., you can choose to replicate full files rather than their chunks. Also, you can use some centralized server to manage the replicas, or you can use a totally distributed design, e.g using Chord to locate a file's replicas or chunks.

The ceiling of your project's grade depends on your design choices:

1. A basic solution using a single centralized server to manage the replicas, as described above, and using TCP has a ceiling of 14 (out of 20), as long as it uses thread-based concurrency. If you use no concurrency, then the ceiling will be of 12.

2. The use of JSSE for secure communication raises the ceiling by up to 2 points (out of 20):

   - SSLSockets - The use of the basic interface provided by SSLSockets/SSLServerSockets will raise the ceiling of your project by 1 point. Note that using this interface may limit the level of concurrency achievable.
   - SSLEngine The use of this more advanced interface will raise your ceiling by 2 points. Note that this interface is also more flexible allowing you to use it together with other APIs to achieve higher concurrency.

3. Addressing each of the following issues, will also raise your ceiling by 2 points (per issue):
   - Scalability - This can be at the design level, e.g. using Chord, or at the implementation level, e.g. using thread-pools and asynchronous I/O, i.e. Java NIO. (If you use Chord and Java NIO with thread-pools, your ceiling will raise by 4 points)
   - Fault-tolerance - The goal is to avoid single-points of failure. E.g. if you choose a centralized server you can replicate it and use, e.g., Paxos or just plain primary-backup. If you choose a decentralized design, you can implement Chord's fault-tolerant features.

## Project usage details

### Compiling

In the 'src/' directory, run: '../scripts/compile.sh'

### Running a peer

Note: No setup is required, because the application creates the needed file
structure by itself.

In the 'src/build/' directory, created in the 'Compiling' step described
above, run: '../../scripts/peer.sh' with the desired arguments.

### Testing a peer

#### RMI

The TestApp connects to a peer through RMI. For this to work, an 'rmiregistry'
has to be started inside the 'src/build/' prior to starting both the Peer and
the TestApp. The access point command line argument of the Peer takes the name
it will use to register itself on the 'rmiregistry', e.g.: 'rmithing'.

By default, the Peer will assume that the rmiregistry has been opened on the
default port. If a different port is needed, it can passed as such: 'rmithing:12345'.
This can be used to have multiple 'rmiregistriy' running at the same time.

In the 'src/build/' directory, created in the 'Compiling' step described
above, run: '../../scripts/test.sh' with the desired arguments.

### Cleaning up a peer's file structure

In the 'src/build/' directory, created in the 'Compiling' step described
above, run: '../../scripts/cleanup.sh' with the desired argument#.
