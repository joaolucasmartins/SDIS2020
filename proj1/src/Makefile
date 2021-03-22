MC=224.0.0.1
MC_P=8081
MDB=224.0.0.2
MDB_P=8082
MDR=224.0.0.3
MDR_P=8083

all:
	javac *.java

clean:
	rm *.class

peer1: all
	java Proj1 1.0 firth rmithing:1029 ${MC} ${MC_P} ${MDB} ${MDB_P} ${MDR} ${MDR_P}

peer2: all
	java Proj1 1.0 secondrth rmithing2:1030 ${MC} ${MC_P} ${MDB} ${MDB_P} ${MDR} ${MDR_P}

test: all
	java TestApp rmithing BACKUP test.txt 3

.PHONY: all clean peer1 peer2 test
