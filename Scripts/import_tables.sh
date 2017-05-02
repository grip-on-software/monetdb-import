#!/bin/bash -e

if [ -z $1 ] || [ "$1" = "--help" ] || [ -z $2 ]; then
	echo "$0 <host> <dump directory> [arguments]"
	exit
fi

HOST=$1
shift
DIRECTORY=$1
shift
ARGUMENTS=$*

ERROR_TEXT="syntax error|current transaction is aborted"

function import() {
	local table=$1
	local error_text="syntax error"

	if [ ! -f "$DIRECTORY/$table.csv.gz" ]; then
		echo "Could not find $DIRECTORY/$table.csv.gz"
		exit 1
	fi

	if [ -f "$DIRECTORY/$table.csv.gz" ]; then
		echo "Importing $table from CSV"
		if [ "$(gzcat $DIRECTORY/$table.csv.gz | mclient -d gros -h $HOST $ARGUMENTS -s "COPY INTO gros.$table FROM STDIN USING DELIMITERS ',', '\n', '\"' NULL AS ''" - 2>&1 | tee /dev/stderr | grep -m 1 -E "$ERROR_TEXT")" ]; then
			exit 1
		fi
		return
	fi
}

echo "Importing schema"
if [ "$(cat $DIRECTORY/schema.sql | mclient -d gros -h $HOST $ARGUMENTS 2>&1 | tee /dev/stderr | grep -m 1 -E "$ERROR_TEXT")" ]; then
	exit 1
fi

LINES=`cat create-tables.sql | grep '^CREATE TABLE' | \
	sed -e 's/CREATE TABLE ".*"\."\(.*\)" (/\1/'`

for line in $LINES
do
	import "$line"
done
