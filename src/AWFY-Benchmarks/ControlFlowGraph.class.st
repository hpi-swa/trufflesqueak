Class {
	#name : #ControlFlowGraph,
	#superclass : #Object,
	#instVars : [
		'basicBlockMap',
		'startNode',
		'edgeList'
	],
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
ControlFlowGraph class >> new [
 ^ super new initialize 
]

{ #category : #'as yet unclassified' }
ControlFlowGraph >> addEdge: edge [

    edgeList append: edge

]

{ #category : #'as yet unclassified' }
ControlFlowGraph >> basicBlocks [

    ^ basicBlockMap

]

{ #category : #'as yet unclassified' }
ControlFlowGraph >> createNode: name [

    | node |

    (basicBlockMap at: name) notNil
      ifTrue:  [ node := basicBlockMap at: name ]
      ifFalse: [
        node := BasicBlock new: name.
        basicBlockMap at: name put: node ].
      
    self numNodes = 1 ifTrue: [startNode := node].
    ^ node 
]

{ #category : #'as yet unclassified' }
ControlFlowGraph >> initialize [

    basicBlockMap := Vector new.
    edgeList := Vector new.

]

{ #category : #'as yet unclassified' }
ControlFlowGraph >> numNodes [

    ^ basicBlockMap size

]

{ #category : #'as yet unclassified' }
ControlFlowGraph >> startBasicBlock [

    ^ startNode

]
