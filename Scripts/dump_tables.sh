#!/bin/bash -ex

if [ -z $1 ] || [ "$1" = "--help" ]; then
	echo "$0 <host> [output directory]"
	exit
fi

HOST=$1

if [ ! -z $2 ]; then
	OUTPUT_DIRECTORY=$2
fi

if [ -z $OUTPUT_DIRECTORY ]; then
	OUTPUT_DIRECTORY=`pwd`
fi

echo $OUTPUT_DIRECTORY

TIMESTAMP=`date +%Y-%m-%d`
DUMP_DIRECTORY="$OUTPUT_DIRECTORY/gros-$TIMESTAMP"

if [ -d $DUMP_DIRECTORY ]; then
	rm -rf $DUMP_DIRECTORY
fi
mkdir $DUMP_DIRECTORY

LARGE_TABLES="issue"
LARGE_DB_DIRECTORY="/home/monetdb"

if [ $HOST = "localhost" ]; then
	LARGE_DB_DIRECTORY="$DUMP_DIRECTORY"
fi

LINES=`cat create-tables.sql | grep '^CREATE TABLE' | \
	sed -e 's/CREATE TABLE ".*"\."\(.*\)" (/\1/'`

for line in $LINES
do
	echo "Dumping $line"
	java -jar ../Code/databasedumper/dist/databasedumper.jar $line $DUMP_DIRECTORY/$line.csv.gz
done

echo "Dumping schema"
msqldump -d gros -h $HOST -D > $DUMP_DIRECTORY/schema.sql
