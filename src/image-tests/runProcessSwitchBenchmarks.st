"Micro-benchmarks for process switching. Must run as startup document in image mode (not via
 --evaluate), so that switches go through the regular scheduler loop:

   trufflesqueak --headless --download-image trufflesqueak-test -- $(pwd)/src/image-tests/runProcessSwitchBenchmarks.st <Benchmark> <param> <outerIterations> <innerIterations>

 Benchmarks (param):
   PingPong (stack depth)      two processes alternate strictly via two semaphores; each switch happens
                               <depth> sends deep and the process returns through all of them afterwards
   YieldRing (stack depth)     8 processes of the same priority calling `Processor yield`
   Handoff (unused)            producer/consumer on a SharedQueue with preemption (signal + wait per item)
   Generator (unused)          Generator next/yield: (context swapping within one process)
   DeepRecursion (depth)       recursion beyond the context stack depth limit (self-flush, no switch)
   Delay (milliseconds)        Delay wait (timer interrupt driven switches)
 Prints one line per outer iteration and a summary with ns per operation."
[
    | out args name param outer inner cls bench times ops sorted |
    out := FileStream stdout.
    args := Smalltalk arguments.
    args size < 4 ifTrue: [
        out nextPutAll: 'usage: <Benchmark> <param> <outerIterations> <innerIterations>, got: ', args printString; cr; flush.
        Smalltalk quitPrimitive: 2].
    name := args first.
    param := (args at: 2) asNumber.
    outer := (args at: 3) asNumber.
    inner := (args at: 4) asNumber.

    "Avoid interactive prompts when compiling methods."
    ToolSet default: CommandLineToolSet.
    Utilities authorName: 'TruffleSqueak'; setAuthorInitials: 'TS'.

    cls := Object subclass: #ProcessSwitchBench
        instanceVariableNames: ''
        classVariableNames: ''
        poolDictionaries: ''
        category: 'ProcessSwitchBench'.
    cls compile: 'descend: depth do: aBlock
        ^ depth <= 1
            ifTrue: [aBlock value]
            ifFalse: [(self descend: depth - 1 do: aBlock) + 1]' classified: 'bench'.
    cls compile: 'pingPong: n depth: depth
        "Answers the number of process switches."
        | semA semB done |
        semA := Semaphore new. semB := Semaphore new. done := Semaphore new.
        [n timesRepeat: [self descend: depth do: [semB wait. semA signal. 0]].
         done signal] forkAt: Processor activePriority.
        n timesRepeat: [self descend: depth do: [semB signal. semA wait. 0]].
        done wait.
        ^ 2 * n' classified: 'bench'.
    cls compile: 'yieldRing: n depth: depth
        | done k |
        k := 8.
        done := Semaphore new.
        k timesRepeat: [
            [n timesRepeat: [self descend: depth do: [Processor yield. 0]].
             done signal] forkAt: Processor activePriority].
        k timesRepeat: [done wait].
        ^ k * n' classified: 'bench'.
    cls compile: 'handoff: n
        | queue sum |
        queue := SharedQueue new.
        [1 to: n do: [:i | queue nextPut: i]] forkAt: Processor activePriority - 1.
        sum := 0.
        n timesRepeat: [sum := sum + queue next].
        sum = (n * (n + 1) / 2) ifFalse: [self error: ''wrong sum''].
        ^ 2 * n' classified: 'bench'.
    cls compile: 'generator: n
        | gen sum |
        gen := Generator on: [:g | 1 to: n do: [:i | g yield: i]].
        sum := 0.
        [gen atEnd] whileFalse: [sum := sum + gen next].
        sum = (n * (n + 1) / 2) ifFalse: [self error: ''wrong sum''].
        ^ 2 * n' classified: 'bench'.
    cls compile: 'deepRecursion: n depth: depth
        n timesRepeat: [(self descend: depth do: [1]) = depth ifFalse: [self error: ''wrong depth'']].
        ^ n * depth' classified: 'bench'.
    cls compile: 'delay: n milliseconds: ms
        | delay |
        delay := Delay forMilliseconds: ms.
        n timesRepeat: [delay wait].
        ^ 2 * n' classified: 'bench'.

    bench := cls new.
    times := OrderedCollection new.
    ops := 0.
    1 to: outer do: [:i | | start elapsed |
        start := Time utcMicrosecondClock.
        ops := name caseOf: {
            ['PingPong'] -> [bench pingPong: inner depth: param].
            ['YieldRing'] -> [bench yieldRing: inner depth: param].
            ['Handoff'] -> [bench handoff: inner].
            ['Generator'] -> [bench generator: inner].
            ['DeepRecursion'] -> [bench deepRecursion: inner depth: param].
            ['Delay'] -> [bench delay: inner milliseconds: param] }.
        elapsed := Time utcMicrosecondClock - start.
        times add: elapsed.
        out nextPutAll: name; nextPutAll: ': iterations=1 runtime: '; print: elapsed; nextPutAll: 'us'; cr; flush].
    sorted := times asSortedCollection asArray.
    out
        nextPutAll: 'RESULT '; nextPutAll: name;
        nextPutAll: ' param='; print: param;
        nextPutAll: ' ops='; print: ops;
        nextPutAll: ' min_ns_per_op='; print: (sorted first * 1000 / ops) rounded;
        nextPutAll: ' median_ns_per_op='; print: ((sorted at: sorted size + 1 // 2) * 1000 / ops) rounded;
        nextPutAll: ' last_ns_per_op='; print: (times last * 1000 / ops) rounded;
        nextPutAll: ' process_switches='; print: (Smalltalk vmParameterAt: 56);
        cr; flush.
    Smalltalk quitPrimitive: 0
]
on: Error
do: [ :ex |
   FileStream stdout cr; nextPutAll: 'Encountered error during benchmark: '; cr; flush.
   ex printVerboseOn: FileStream stdout.
   FileStream stdout cr; flush.
   Smalltalk quitPrimitive: 1
]
