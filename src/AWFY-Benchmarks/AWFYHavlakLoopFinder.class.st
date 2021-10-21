Class {
	#name : #AWFYHavlakLoopFinder,
	#superclass : #Object,
	#instVars : [
		'cfg',
		'lsg',
		'nonBackPreds',
		'backPreds',
		'number',
		'maxSize',
		'header',
		'type',
		'last',
		'nodes'
	],
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
AWFYHavlakLoopFinder class >> new: cfg lsg: lsg [

    ^ self new init: cfg lsg: lsg

]

{ #category : #'as yet unclassified' }
AWFYHavlakLoopFinder >> MaxNonBackPreds [
 ^ 32 * 1024 
]

{ #category : #'as yet unclassified' }
AWFYHavlakLoopFinder >> Unvisited [
 ^ 2147483647 
]

{ #category : #'as yet unclassified' }
AWFYHavlakLoopFinder >> doDFS: currentNode current: current [

    | lastId outerBlocks |
    
    (nodes at: current) initNode: currentNode dfs: current.
    number at: currentNode put: current.
    
    lastId := current.
    outerBlocks := currentNode outEdges.
    
    1 to: outerBlocks size do: [:i |
      | target |
      target := outerBlocks at: i.
      (number at: target) = self Unvisited ifTrue: [
        lastId := self doDFS: target current: lastId + 1 ] ].
    
    last at: current put: lastId.
    ^ lastId

]

{ #category : #'as yet unclassified' }
AWFYHavlakLoopFinder >> findLoops [

    | size |
    cfg startBasicBlock isNil ifTrue: [ ^ self ].
    
    size := cfg numNodes.
    
    nonBackPreds removeAll.
    backPreds removeAll.
    number removeAll.
    
    size > maxSize ifTrue: [
      header := Array new: size.
      type   := Array new: size.
      last   := Array new: size.
      nodes  := Array new: size.
      maxSize := size ].

    1 to: size do: [:i |
      nonBackPreds append: AWFYSomSet new.
      backPreds append: AWFYVector new.
      nodes at: i put: AWFYUnionFindNode new ].

    self initAllNodes.
    self identifyEdges: size.
    header at: 1 put: 1.
    
    size to: 1 by: -1 do: [:w |
      | nodePool nodeW |
      nodePool := AWFYVector new.
      nodeW := (nodes at: w) bb.
      
      nodeW notNil ifTrue: [
        | workList |
        self stepD: w nodePool: nodePool.
        
        workList := AWFYVector new.
        nodePool forEach: [:niter | workList append: niter ].
        
        nodePool size ~= 0 ifTrue: [
          type at: w put: #BBReducible.
        ].
        
        [ workList isEmpty ] whileFalse: [
          | x nonBackSize |
          x := workList removeFirst.
          
          nonBackSize := (nonBackPreds at: x dfsNumber) size.
          nonBackSize > self MaxNonBackPreds ifTrue: [ ^ self ].
          self stepEProcessNonBackPreds: w nodePool: nodePool workList: workList x: x ].
          
        (nodePool size > 0 or: [(type at: w) = #BBSelf]) ifTrue: [
          | loop |
          loop := lsg createNewLoop: nodeW reducible: ((type at: w) ~= #BBIrreducible).
          self setLoopAttribute: w nodePool: nodePool loop: loop ] ] ]

]

{ #category : #'as yet unclassified' }
AWFYHavlakLoopFinder >> identifyEdges: size [

    1 to: size do: [:w |
      | nodeW |
      header at: w put: 1.
      type   at: w put: #BBNonHeader.
      
      nodeW := (nodes at: w) bb.
      nodeW isNil
        ifTrue:  [ type at: w put: #BBDead ]
        ifFalse: [ self processEdges: nodeW w: w ] ]

]

{ #category : #'as yet unclassified' }
AWFYHavlakLoopFinder >> init: aCfg lsg: aLsg [

    cfg := aCfg.
    lsg := aLsg.
    
    nonBackPreds := AWFYVector new.
    backPreds    := AWFYVector new.
    number := AWFYSomIdentityDictionary new.

    maxSize := 0.

]

{ #category : #'as yet unclassified' }
AWFYHavlakLoopFinder >> initAllNodes [

    cfg basicBlocks forEach: [:bb |
      number at: bb put: self Unvisited ].

    self doDFS: cfg startBasicBlock current: 1

]

{ #category : #'as yet unclassified' }
AWFYHavlakLoopFinder >> isAncestor: w v: v [

    ^ (w <= v) and: [(v <= (last at: w))]

]

{ #category : #'as yet unclassified' }
AWFYHavlakLoopFinder >> processEdges: nodeW w: w [

    nodeW numPred > 0 ifTrue: [
      nodeW inEdges forEach: [:nodeV |
        | v |
        v := number at: nodeV.
        v ~= self Unvisited ifTrue: [
          (self isAncestor: w v: v)
            ifTrue:  [ (backPreds at: w) append: v ]
            ifFalse: [ (nonBackPreds at: w) add: v ] ] ] ]

]

{ #category : #'as yet unclassified' }
AWFYHavlakLoopFinder >> setLoopAttribute: w nodePool: nodePool loop: loop [

    (nodes at: w) loop: loop.

    nodePool forEach: [:node |
      header at: node dfsNumber put: w.
      node union: (nodes at: w).
      
      node loop notNil
        ifTrue: [ node loop parent: loop ]
        ifFalse: [ loop addNode: node bb ] ]

]

{ #category : #'as yet unclassified' }
AWFYHavlakLoopFinder >> stepD: w nodePool: nodePool [

    (backPreds at: w) forEach: [:v |
      v ~= w ifTrue: [ nodePool append: (nodes at: v) findSet ]
             ifFalse: [ type at: w put: #BBSelf ] ]

]

{ #category : #'as yet unclassified' }
AWFYHavlakLoopFinder >> stepEProcessNonBackPreds: w nodePool: nodePool workList: workList x: x [

    (nonBackPreds at: x dfsNumber) forEach: [:iter |
      | y ydash |
      y := nodes at: iter.
      ydash := y findSet.
      
      (self isAncestor: w v: ydash dfsNumber) not
        ifTrue: [
          type at: w put: #BBIrreducible.
          (nonBackPreds at: w) add: ydash dfsNumber ]
        ifFalse: [
          ydash dfsNumber ~= w ifTrue: [
            (nodePool hasSome: [:e | e == ydash]) ifFalse: [
              workList append: ydash.
              nodePool append: ydash ] ] ] ]

]
