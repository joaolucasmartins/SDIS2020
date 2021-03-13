all:
	javac *.java

clean:
	rm *.class

peer1: all
	java Proj1 1.0 firth mrithing 224.0.0.1 8081 224.0.0.2 8082 224.0.0.3 8083

peer2: all
	java Proj1 1.0 secondrth mrithing 224.0.0.1 8081 224.0.0.2 8082 224.0.0.3 8083

.PHONY: all clean peer1 peer2
