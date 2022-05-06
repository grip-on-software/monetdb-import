#!/bin/bash

# MonetDB database dump importer.
#
# Copyright 2017-2020 ICTU
# Copyright 2017-2022 Leiden University
# 
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
# 
#     http://www.apache.org/licenses/LICENSE-2.0
# 
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

set -e

if [ -z $1 ] || [ "$1" = "--help" ] || [ -z $2 ]; then
	echo "$0 <host> <dump directory> [dbname] [schema] [arguments]"
	echo ""
	echo "Perform an import of a database dump made by dump_tables.sh and/or"
	echo "monetdb-dumper from a directory into a created, but empty database."
	echo "The dump directory contains a schema.sql and <table>.sql.gz and/or"
	echo "<table>.csv.gz files for all the tables in the schema."
	echo "The database name and schema are both by default 'gros'."
	echo "Additional arguments are passed to the mclient command when importing"
	echo "the schema or a table."
	exit
fi

HOST=$1
shift
DIRECTORY=$1
shift
if [ ! -z "$1" ]; then
	DATABASE=$1
	shift
else
	DATABASE="gros"
fi
if [ ! -z "$1" ]; then
	SCHEMA=$1
	shift
else
	SCHEMA="gros"
fi
ARGUMENTS=$@

ERROR_TEXT="syntax error|current transaction is aborted"

function import() {
	local table=$1
	local error_text="syntax error"

	if [ -f "$DIRECTORY/$table.sql.gz" ]; then
		echo "Importing $table from SQL"
		if [ "$(gzcat "$DIRECTORY/$table.sql.gz" | mclient -d "$DATABASE" -h "$HOST" $ARGUMENTS 2>&1 | tee >(cat>&2) | grep -m 1 -E "$ERROR_TEXT")" ]; then
			exit 1
		fi
	elif [ -f "$DIRECTORY/$table.csv.gz" ]; then
		echo "Importing $table from CSV"
		if [ "$(gzcat "$DIRECTORY/$table.csv.gz" | mclient -d "$DATABASE" -h "$HOST" $ARGUMENTS -s "COPY INTO $SCHEMA.$table FROM STDIN USING DELIMITERS ',', '\n', '\"' NULL AS '\\007NUL\\007'" - 2>&1 | tee >(cat>&2) | grep -m 1 -E "$ERROR_TEXT")" ]; then
			exit 1
		fi
	else
		echo "Could not find $DIRECTORY/$table.sql.gz or $DIRECTORY/$table.csv.gz"
		exit 1
	fi
}

echo "Importing schema"
if [ "$(cat "$DIRECTORY/schema.sql" | mclient -d "$DATABASE" -h "$HOST" $ARGUMENTS 2>&1 | tee >(cat>&2) | grep -m 1 -E "$ERROR_TEXT")" ]; then
	exit 1
fi

LINES=`cat "$DIRECTORY/schema.sql" | grep "^CREATE TABLE \"$SCHEMA\"" | \
	sed -e 's/CREATE TABLE ".*"\."\(.*\)" (/\1/'`

for line in $LINES
do
	import "$line"
done
