#!/bin/bash -e

if [ -z $1 ] || [ "$1" = "--help" ]; then
	echo "$0 <host|--help|--dry-run> [output directory] [dumper path] [dumper config]"
	exit
fi

SCRIPT_DIR=$(dirname "${BASH_SOURCE[0]}")

HOST="$1"

if [ ! -z "$2" ]; then
	OUTPUT_DIRECTORY="$2"
fi
if [ ! -z "$3" ]; then
	DUMPER_PATH="$3"
fi
if [ ! -z "$4" ]; then
	DUMPER_CONFIG="$4"
fi

if [ -z "$OUTPUT_DIRECTORY" ]; then
	OUTPUT_DIRECTORY=`pwd`
fi
if [ -z "$DUMPER_PATH" ]; then
	DUMPER_PATH="$SCRIPT_DIR/../../monetdb-dumper/dist/databasedumper.jar"
	DUMPER_CONFIG="$SCRIPT_DIR/../../monetdb-dumper/nbproject/private/config.properties"
fi

TIMESTAMP=`date +%Y-%m-%d`
DUMP_DIRECTORY="$OUTPUT_DIRECTORY/gros-$TIMESTAMP"

echo "HOST=$HOST"
echo "SCRIPT_DIR=$SCRIPT_DIR"
echo "DUMPER_PATH=$DUMPER_PATH"
echo "DUMP_DIRECTORY=$DUMP_DIRECTORY"

if [ "$HOST" = "--dry-run" ]; then
	echo "DRY RUN"
else
	if [ ! -z "$DUMPER_CONFIG" ]; then
		if [ ! $(grep databasedumper.url "$DUMPER_CONFIG" | grep -- "$HOST") ]; then
			echo "Make sure the database dumper is configured for the host."
			exit 1
		fi
	fi

	if [ -d "$DUMP_DIRECTORY" ]; then
		rm -rf "$DUMP_DIRECTORY"
	fi
	mkdir "$DUMP_DIRECTORY"
fi

LINES=`cat "$SCRIPT_DIR/create-tables.sql" | grep '^CREATE TABLE' | \
	sed -e 's/CREATE TABLE ".*"\."\(.*\)" (/\1/'`

for line in $LINES
do
	echo "Dumping $line"
	if [ "$HOST" != "--dry-run" ]; then
		java -jar $DUMPER_PATH $line "$DUMP_DIRECTORY/$line.csv.gz"
	fi
done

echo "Dumping schema"
if [ "$HOST" != "--dry-run" ]; then
	msqldump -d gros -h $HOST -D > "$DUMP_DIRECTORY/schema.sql"
fi
