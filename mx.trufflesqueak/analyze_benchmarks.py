#
# Copyright (c) 2025 Software Architecture Group, Hasso Plattner Institute
# Copyright (c) 2025 Oracle and/or its affiliates
#
# Licensed under the MIT License.
#

import math
import os
import re
import sys

from statistics import median_low
from enum import Enum
from dataclasses import dataclass, field

BENCHMARKS = [
    "Bounce",
    "CD",
    "DeltaBlue",
    "Havlak",
    "Json",
    "List",
    "Mandelbrot",
    "NBody",
    "Queens",
    "Permute",
    "Richards",
    "Sieve",
    "Storage",
    "Towers",
]

RE_LOG_LINE = re.compile(
    r"^(?:.*: )?([^\s]+)( [\w\.]+)?: iterations=([0-9]+) "
    + r"runtime: (?P<runtime>(\d+(\.\d*)?|\.\d+)([eE][-+]?\d+)?)"
    + r"(?P<unit>[mu])s"
)

RE_COMPILATION_SUMMARY_LINE = re.compile(
    r"\s*(.+?)\s*:\s*"
    r"count=\s*(\d+),\s*"
    r"sum=\s*(\d+),\s*"
    r"min=\s*(\d+),\s*"
    r"average=\s*([\d.]+),\s*"
    r"max=\s*(\d+),\s*"
    r"maxTarget=(.+)"
)

IS_PEAK = sys.argv[1] == "peak"
WARMUP_ITERATIONS = 50 if IS_PEAK else 10
PEAK_ITERATIONS = 200


def print_peak_summary(results):
    print(
        '| Benchmark | [Min](# "Smallest value in ms") | [Med](# "Low median in ms") | [Max](# "Largest value in ms") | [:stopwatch:](# "Total time in mm:ss.ss") | [:fire:](# "First Stable Iteration") | [:bulb:](# "Compilations") | [:wastebasket:](# "Invalidations") | [:dna:](# "Splits") | [Nodes](# "Truffle Node Count") | [Tier 1](# "Tier 1: Total Code Size") | [Tier 2](# "Tier 2: Total Code Size") | [Memory](# "Peak RSS in MB") |'  # pylint: disable=line-too-long
    )
    print("|:-- | --:| --:| --:| -- | --:| --:| --:| --:| --:| --:| --:| --:| ")
    sums = [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
    for r in results.values():
        r_min = r.min()
        r_median_low = r.median_low()
        r_max = r.max()
        r_time_s = r.time_s()
        print(
            f"| {r.bench_name} | {r_min} | {r_median_low} | {r_max} | {mm_ss(r_time_s)} | {r.first_stable_iteration} | {r.compilations} | {r.invalidations} | {r.splits} | {r.node_count} | {r.t1_code_size} | {r.t2_code_size} | {r.peak_rss} |"
        )
        sums = [
            x + y
            for x, y in zip(
                sums,
                [
                    r_min,
                    r_median_low,
                    r_max,
                    r_time_s,
                    r.first_stable_iteration,
                    r.compilations,
                    r.invalidations,
                    r.splits,
                    r.node_count,
                    r.t1_code_size,
                    r.t2_code_size,
                    r.peak_rss,
                ],
            )
        ]
    print(
        f"| | {sums[0]} | {sums[1]} | {sums[2]} | {mm_ss(sums[3])} | {sums[4]} | {sums[5]} | {sums[6]} | {sums[7]} | {sums[8]} | {sums[9]} | {sums[10]} | {sums[11]} |"
    )


def print_interpreter_summary(results):
    print(
        '| Benchmark | [Min](# "Smallest value in ms") | [Med](# "Low median in ms") | [Max](# "Largest value in ms") | [:stopwatch:](# "Total time in mm:ss.ss") | [Memory](# "Peak RSS in MB") |'  # pylint: disable=line-too-long
    )
    print("|:-- | --:| --:| --:| -- | --:| ")
    sums = [
        0,
        0,
        0,
        0,
        0,
    ]
    for r in results.values():
        r_min = r.min()
        r_median_low = r.median_low()
        r_max = r.max()
        r_time_s = r.time_s()
        print(
            f"| {r.bench_name} | {r_min} | {r_median_low} | {r_max} | {mm_ss(r_time_s)} | {r.peak_rss} |"
        )
        sums = [
            x + y
            for x, y in zip(
                sums,
                [
                    r_min,
                    r_median_low,
                    r_max,
                    r_time_s,
                    r.peak_rss,
                ],
            )
        ]
    print(f"| | {sums[0]} | {sums[1]} | {sums[2]} | {mm_ss(sums[3])} | {sums[4]} |")


def mm_ss(seconds):
    minutes = int(seconds // 60)
    secs = seconds % 60
    return f"{minutes:02}:{secs:05.2f}"


def to_int(value):
    int_value = int(value)
    assert int_value == value
    return int_value


def print_warmup(r):
    if IS_PEAK:
        print(f"## Warmup")

    print(
        f"""
```mermaid
---
config:
    themeVariables:
        xyChart:
            plotColorPalette: '#1f77b4, #ff7f0e, #2ca02c, #d62728, #9467bd, #8c564b, #e377c2, #7f7f7f, #bcbd22, #17becf'
---
xychart-beta
    title "First {WARMUP_ITERATIONS} Iterations"
    y-axis "Time (in ms)" {min([min(r[bench_name].warmup_iterations()) for bench_name in BENCHMARKS])} --> {max([max(r[bench_name].warmup_iterations()) for bench_name in BENCHMARKS])}
    """
    )
    for bench_name in BENCHMARKS:
        warmup_values = r[bench_name].warmup_iterations()
        print(f"line [{', '.join([str(x) for x in warmup_values])}]")
    print(
        """
```
        """
    )


def print_steady(r):
    print(f"## Steady")

    for bench_name in BENCHMARKS:
        peak_values = r[bench_name].peak_iterations()
        num_peak_values = len(peak_values)
        print(
            f"""
```mermaid
---
config:
    xyChart:
        height: 300
    themeVariables:
        xyChart:
            plotColorPalette: '#1f77b4'
---
xychart-beta
    title "{bench_name}"
    x-axis "Last {num_peak_values} Iterations" {PEAK_ITERATIONS} --> {PEAK_ITERATIONS + num_peak_values}
    y-axis "Time (in ms)" {min(peak_values)} --> {max(peak_values)}
    line [{', '.join([str(x) for x in peak_values])}]
```
        """
        )


@dataclass
class Result:
    bench_name: str
    values: list[int] = field(default_factory=list)
    first_stable_iteration: int = -1
    compilations: int = -1
    invalidations: int = -1
    splits: int = -1
    node_count: int = -1
    t1_code_size: int = -1
    t2_code_size: int = -1
    peak_rss: float = -1

    def min(self):
        return min(self.values)

    def median_low(self):
        return median_low(self.values)

    def max(self):
        return max(self.values)

    def time_s(self):
        return round(
            sum([v / 1000 for v in self.values]), 2  # pylint: disable=not-an-iterable
        )

    def warmup_iterations(self):
        return self.values[:WARMUP_ITERATIONS]

    def peak_iterations(self):
        return self.values[PEAK_ITERATIONS:]


class Phase(Enum):
    run_time = 1
    truffle_runtime_stats = 2
    truffle_ast_stats = 3
    truffle_tier_1 = 4
    truffle_tier_2 = 5
    time_verbose = 6


def get_result(bench_name):
    log_name = f"{bench_name}.log"
    if not os.path.isfile(log_name):
        return Result(bench_name)
    values = []
    first_stable_iteration = -1
    compilations = -1
    invalidations = -1
    splits = -1
    node_count = -1
    t1_code_size = -1
    t2_code_size = -1
    peak_rss = -1

    with open(log_name) as file:
        iteration = 0
        phase = Phase.run_time
        for line in file:
            if phase == Phase.run_time:
                match = RE_LOG_LINE.match(line)
                if match:
                    iteration += 1
                    assert bench_name == match.group(1)
                    time = float(match.group("runtime"))
                    if match.group("unit") == "u":
                        time /= 1000
                    values.append(to_int(time))
                elif line.startswith("[engine] opt done"):
                    first_stable_iteration = iteration
                elif line.startswith("[engine] Truffle runtime statistics"):
                    phase = Phase.truffle_runtime_stats
                elif "Command being timed" in line:
                    phase = Phase.time_verbose
            elif phase == Phase.truffle_runtime_stats:
                if "Compilations" in line:
                    compilations = int(line.split(":")[1].strip())
                if "Invalidated" in line:
                    invalidations = int(line.split(":")[1].strip())
                elif "Splits" in line:
                    splits = int(line.split(":")[1].strip())
                elif "AST node statistics" in line:
                    phase = Phase.truffle_ast_stats
            elif phase == Phase.truffle_ast_stats:
                if "Truffle node count" in line:
                    match = RE_COMPILATION_SUMMARY_LINE.match(line)
                    node_count = int(match.group(3))
                elif "Compilation Tier 1" in line:
                    phase = Phase.truffle_tier_1
            elif phase == Phase.truffle_tier_1:
                if "Code size" in line:
                    match = RE_COMPILATION_SUMMARY_LINE.match(line)
                    t1_code_size = int(match.group(3))
                elif "Data references" in line:
                    phase = Phase.truffle_tier_2
            elif phase == Phase.truffle_tier_2:
                if "Code size" in line:
                    match = RE_COMPILATION_SUMMARY_LINE.match(line)
                    t2_code_size = int(match.group(3))
                elif "Data references" in line:
                    phase = Phase.time_verbose
            elif phase == Phase.time_verbose:
                if "Maximum resident set size (kbytes): " in line:
                    peak_rss = math.ceil(float(line.split(": ")[1]) / 1000)
            else:
                raise ValueError(f"Unexpectedd phase: {phase}")

    return Result(
        bench_name,
        values,
        first_stable_iteration,
        compilations,
        invalidations,
        splits,
        node_count,
        t1_code_size,
        t2_code_size,
        peak_rss,
    )


def main():
    results = {}
    for bench_name in BENCHMARKS:
        results[bench_name] = get_result(bench_name)

    print(f"# {'Peak' if IS_PEAK else 'Intepreter'} Performance Report\n")
    if IS_PEAK:
        print_peak_summary(results)
    else:
        print_interpreter_summary(results)
    print_warmup(results)
    if IS_PEAK:
        print_steady(results)

    return 0


if __name__ == "__main__":
    sys.exit(main())
