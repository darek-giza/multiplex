#!/bin/bash

echo "Starting multiplex api ..."
exec sbt clean reload compile run
