Class {
	#name : #AWFYLoopTesterApp,
	#superclass : #Object,
	#instVars : [
		'cfg',
		'lsg'
	],
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
AWFYLoopTesterApp class >> new [
 ^ super new initialize 
]

{ #category : #'as yet unclassified' }
AWFYLoopTesterApp >> addDummyLoops: numDummyLoops [

    numDummyLoops timesRepeat: [
      self findLoops: lsg ]

]

{ #category : #'as yet unclassified' }
AWFYLoopTesterApp >> buildBaseLoop: from [

    | header diamond1 d11 diamond2 footer |
    header   := self buildStraight: from n: 1.
    diamond1 := self buildDiamond: header.
    d11      := self buildStraight: diamond1 n: 1.
    diamond2 := self buildDiamond: d11.
    footer   := self buildStraight: diamond2 n: 1.
    
    self buildConnect: diamond2 end: d11.
    self buildConnect: diamond1 end: header.
    self buildConnect: footer   end: from.
    footer := self buildStraight: footer n: 1.
    ^ footer

]

{ #category : #'as yet unclassified' }
AWFYLoopTesterApp >> buildConnect: start end: end [

    AWFYBasicBlockEdge for: cfg from: start to: end

]

{ #category : #'as yet unclassified' }
AWFYLoopTesterApp >> buildDiamond: start [

    | bb0 |
    bb0 := start.
    AWFYBasicBlockEdge for: cfg from: bb0 to: bb0 + 1.
    AWFYBasicBlockEdge for: cfg from: bb0 to: bb0 + 2.
    AWFYBasicBlockEdge for: cfg from: bb0 + 1 to: bb0 + 3.
    AWFYBasicBlockEdge for: cfg from: bb0 + 2 to: bb0 + 3.
    ^ bb0 + 3

]

{ #category : #'as yet unclassified' }
AWFYLoopTesterApp >> buildStraight: start n: n [

    0 to: n - 1 do: [:i |
      self buildConnect: start + i end: start + i + 1 ].
    ^ start + n

]

{ #category : #'as yet unclassified' }
AWFYLoopTesterApp >> constructCFG: parLoops p: pparLoops p: ppparLoops [

    | n |
    n := 3.
    
    parLoops timesRepeat: [
      cfg createNode: n + 1.
      self buildConnect: 2 end: n + 1.
      n := n + 1.
      
      pparLoops timesRepeat: [
        | top bottom |
        top := n.
        n := self buildStraight: n n:1.
        ppparLoops timesRepeat: [ n := self buildBaseLoop: n ].
        bottom := self buildStraight: n n: 1.
        self buildConnect: n end: top.
        n := bottom ].
      
      self buildConnect: n end: 1 ]

]

{ #category : #'as yet unclassified' }
AWFYLoopTesterApp >> constructSimpleCFG [

    cfg createNode: 1.
    self buildBaseLoop: 1.
    cfg createNode: 2.
    AWFYBasicBlockEdge for: cfg from: 1 to:  3

]

{ #category : #'as yet unclassified' }
AWFYLoopTesterApp >> findLoops: loopStructure [

    | finder |
    finder := AWFYHavlakLoopFinder new: cfg lsg: loopStructure.
    finder findLoops

]

{ #category : #'as yet unclassified' }
AWFYLoopTesterApp >> initialize [

    cfg := AWFYControlFlowGraph new.
    lsg := AWFYLoopStructureGraph new.
    cfg createNode: 1.

]

{ #category : #'as yet unclassified' }
AWFYLoopTesterApp >> main: numDummyLoops loop: findLoopIterations p: parLoop p: pparLoops p: ppparLoops [

    self constructSimpleCFG.
    self addDummyLoops: numDummyLoops.
    self constructCFG: parLoop p: pparLoops p: ppparLoops.
    
    self findLoops: lsg.
    findLoopIterations timesRepeat: [
      self findLoops: AWFYLoopStructureGraph new ].
  
    lsg calculateNestingLevel.
    ^ Array with: lsg numLoops with: cfg numNodes

]
