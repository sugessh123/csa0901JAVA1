#!/bin/sh
# Runs ScanOrder 12 times in separate JVMs (default priorities) and 12 times with priorities changed.
echo "--- default priorities ---"
for i in $(seq 1 12); do printf "run %2d: " $i; java ScanOrder; done
echo "--- S1=MIN, S2=MAX, S3/S4=NORM ---"
for i in $(seq 1 12); do printf "run %2d: " $i; java ScanOrder priority; done
