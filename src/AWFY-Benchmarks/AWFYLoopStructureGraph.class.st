Class {
	#name : #AWFYLoopStructureGraph,
	#superclass : #Object,
	#instVars : [
		'root',
		'loops',
		'loopCounter'
	],
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
AWFYLoopStructureGraph class >> new [

    ^ super new initialize

]

{ #category : #'as yet unclassified' }
AWFYLoopStructureGraph >> calculateNestingLevel [

    loops forEach: [:liter |
      liter isRoot ifFalse: [
        liter parent isNil ifTrue: [
          liter parent: root ] ] ].

    self calculateNestingLevelRec: root depth: 0

]

{ #category : #'as yet unclassified' }
AWFYLoopStructureGraph >> calculateNestingLevelRec: loop depth: depth [

    loop depthLevel: depth.
    loop children forEach: [:liter |
      self calculateNestingLevelRec: liter depth: depth + 1.
      loop nestingLevel: (loop nestingLevel max: 1 + liter nestingLevel) ]

]

{ #category : #'as yet unclassified' }
AWFYLoopStructureGraph >> createNewLoop: bb reducible: isReducible [

    | loop |
    loop := AWFYSimpleLoop basicBlock: bb reducible: isReducible.
    loop counter: loopCounter.
    loopCounter := loopCounter + 1.
    loops append: loop.
    ^ loop

]

{ #category : #'as yet unclassified' }
AWFYLoopStructureGraph >> initialize [

    root  := AWFYSimpleLoop basicBlock: nil reducible: false.
    loops := AWFYVector new.
    loopCounter := 0.
  
    root nestingLevel: 0.
    root counter: loopCounter.
    loopCounter := loopCounter + 1.
    loops append: root

]

{ #category : #'as yet unclassified' }
AWFYLoopStructureGraph >> numLoops [

    ^ loops size

]
