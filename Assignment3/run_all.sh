#!/usr/bin/env bash
# Compiles and runs every program of Assignment 3 and stores the console output in Qn/output/.
# Usage:  ./run_all.sh            (full run, about 6-8 minutes, includes all measurements)
#         ./run_all.sh --quick    (skips the long experiments, about 1 minute)
# Requires a JDK 17 or later (javac + java) on the PATH.
set -u
QUICK=0; [ "${1:-}" = "--quick" ] && QUICK=1
command -v javac >/dev/null || { echo "javac not found - install a JDK 17+"; exit 1; }
ROOT="$(cd "$(dirname "$0")" && pwd)"
java -version 2>&1 | head -1
run() { # run <dir> <outfile> <command...>
  local d="$1" out="$2"; shift 2
  mkdir -p "$ROOT/$d/output"
  echo "  -> $d: $*"
  ( cd "$ROOT/$d" && "$@" ) > "$ROOT/$d/output/$out" 2>&1
}
cc() { ( cd "$ROOT/$1" && shift && javac "$@" ) || { echo "COMPILE FAILED in $1"; exit 1; }; }

echo "== Q1"; cc Q1 AtmWithdrawal.java UncaughtDemo.java ErrorCodeAtm.java
run Q1 atm.txt java AtmWithdrawal
run Q1 uncaught_stacktrace.txt java UncaughtDemo
( cd "$ROOT/Q1" && javac -d /tmp/q1_broken broken/BrokenCatchOrder.java ) > "$ROOT/Q1/output/broken_catch_order.txt" 2>&1
run Q1 error_code_vs_exception.txt java ErrorCodeAtm

echo "== Q2"
mkdir -p /tmp/q2_a /tmp/q2_b
( cd "$ROOT/Q2/complete_code" && javac -d /tmp/q2_a StorageDemo.java && javac -d /tmp/q2_b StorageDemoNoCause.java ) || exit 1
mkdir -p "$ROOT/Q2/output"
java -cp /tmp/q2_a StorageDemo > "$ROOT/Q2/output/storagedemo.txt" 2>&1
java -cp /tmp/q2_b StorageDemoNoCause > "$ROOT/Q2/output/storagedemo_nocause.txt" 2>&1
cc Q2/portal ExamPortal.java ExceptionCostBench.java
run Q2/portal portal.txt java ExamPortal
run Q2/portal exception_cost.txt java ExceptionCostBench

echo "== Q3"; cc Q3 SalesReport.java SuppressedDemo.java FdExhaustionDemo.java
run Q3 sales_report.txt java SalesReport
run Q3 suppressed.txt java SuppressedDemo
( ulimit -n 100; cd "$ROOT/Q3" && java -Xmx2g FdExhaustionDemo noclose 2>&1 | sed -n '1p;$p'; java FdExhaustionDemo twr 2>&1 | tail -1 ) > "$ROOT/Q3/output/too_many_open_files.txt" 2>&1

echo "== Q4"; cc Q4 ScanStations.java ScanOrder.java DaemonDemo.java PoolCompare.java PriorityStats.java
run Q4 scan_stations.txt java ScanStations
( cd "$ROOT/Q4" && { echo "-- S4 normal thread:"; java DaemonDemo; echo "-- S4 daemon thread:"; java DaemonDemo daemon; } ) > "$ROOT/Q4/output/daemon.txt" 2>&1
run Q4 pool_compare.txt java PoolCompare
if [ $QUICK -eq 0 ]; then run Q4 order_runs.txt sh ./run10.sh; run Q4 priority_stats.txt java PriorityStats; fi

echo "== Q5"
mkdir -p /tmp/q5_sync /tmp/q5_unsync
( cd "$ROOT/Q5" && javac -d /tmp/q5_sync complete_code/SeatBooking.java && javac -d /tmp/q5_unsync unsync/SeatBooking.java ) || exit 1
mkdir -p "$ROOT/Q5/output"
{ echo "=== UNSYNCHRONIZED, 10 runs ==="; for i in 1 2 3 4 5 6 7 8 9 10; do printf "run %2d: " $i; java -cp /tmp/q5_unsync SeatBooking; done
  echo "=== SYNCHRONIZED (TODO 1 completed), 10 runs ==="; for i in 1 2 3 4 5 6 7 8 9 10; do printf "run %2d: " $i; java -cp /tmp/q5_sync SeatBooking; done; } > "$ROOT/Q5/output/seatbooking_runs.txt" 2>&1
cc Q5 Bookers.java RaceTest.java SyncBench.java StaticSyncDemo.java SpaceCost.java
run Q5 race_test.txt java RaceTest
run Q5 static_sync.txt java StaticSyncDemo
run Q5 space_cost.txt java SpaceCost
run Q5 sync_bench.txt java SyncBench

echo "== Q6"
mkdir -p /tmp/q6_while /tmp/q6_if
( cd "$ROOT/Q6/partB" && javac -d /tmp/q6_while complete_code/TokenCounter.java && javac -d /tmp/q6_if if_variant/TokenCounter.java ) || exit 1
cc Q6/partA Transfers.java; run Q6/partA transfers.txt java Transfers
cc Q6/partB TokenCounterTraced.java Experiments.java
mkdir -p "$ROOT/Q6/partB/output"
N=100; [ $QUICK -eq 1 ] && N=10
count() { ok=0; wrong=0; hung=0; for i in $(seq 1 $N); do out=$(timeout 5 java -cp "$1" TokenCounter 2>&1); rc=$?; if [ $rc -eq 124 ]; then hung=$((hung+1)); elif echo "$out" | grep -q "collected: 210 "; then ok=$((ok+1)); else wrong=$((wrong+1)); fi; done; echo "$N runs: correct(210)=$ok wrong=$wrong hung=$hung"; }
{ echo "=== with while ==="; java -cp /tmp/q6_while TokenCounter; count /tmp/q6_while; echo "=== with if ==="; count /tmp/q6_if; } > "$ROOT/Q6/partB/output/complete_code.txt" 2>&1
run Q6/partB experiments.txt java Experiments

echo "== Q7"; cc Q7 ResultProcessor.java DataGen.java HandCheck.java Measure.java Alternatives.java
run Q7 handcheck.txt java HandCheck
if [ $QUICK -eq 0 ]; then
  run Q7 pool_measurements.txt java Measure
  ( cd "$ROOT/Q7" && for m in executor8 stream-catch stream-nocatch thread-per-file thread-per-file-2s; do echo "######## $m"; timeout 120 java Alternatives $m 2>&1 | tail -6; done ) > "$ROOT/Q7/output/alternatives.txt" 2>&1
fi
echo "Done. Outputs are in each Qn/output/ folder. Take your own screenshots of the console runs for the report."
