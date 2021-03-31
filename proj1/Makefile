MC=224.0.0.1
MC_P=8081
MDB=224.0.0.2
MDB_P=8082
MDR=224.0.0.3
MDR_P=8083

src_dir=src

all:
	javac ${src_dir}/*.java ${src_dir}/File/*.java ${src_dir}/Message/*.java ${src_dir}/utils/*.java

clean:
	rm -f ${src_dir}/*.class ${src_dir}/File/*.class ${src_dir}/Message/*.class ${src_dir}/utils/*.class

peer1: all
	java -classpath ${src_dir} Proj1 1.0 firth rmithing:1029 ${MC} ${MC_P} ${MDB} ${MDB_P} ${MDR} ${MDR_P}

peer2: all
	java -classpath ${src_dir} Proj1 1.0 secondrth rmithing2:1030 ${MC} ${MC_P} ${MDB} ${MDB_P} ${MDR} ${MDR_P}

test_backup: all
	java ${src_dir}/TestApp rmithing:1029 BACKUP filename.txt 3

test_del: all
	java ${src_dir}/TestApp rmithing:1029 DELETE filename.txt

.PHONY: all clean peer1 peer2 test
