all:
	javac *.java

clean:
	rm *.class

peer1: all
	java Proj1 1.0 firth rmithing 224.0.0.1 8081 224.0.0.2 8082 224.0.0.3 8083

peer2: all
	java Proj1 1.0 secondrth rmithing2 224.0.0.1 8081 224.0.0.2 8082 224.0.0.3 8083

test: all
	java TestApp rmithing BACKUP test.txt 3

.PHONY: all clean peer1 peer2 test
