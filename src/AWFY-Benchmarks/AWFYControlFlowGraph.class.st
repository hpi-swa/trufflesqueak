Class {
	#name : #AWFYControlFlowGraph,
	#superclass : #Object,
	#instVars : [
		'basicBlockMap',
		'startNode',
		'edgeList'
	],
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
AWFYControlFlowGraph class >> new [
 ^ super new initialize 
]

{ #category : #'as yet unclassified' }
AWFYControlFlowGraph >> addEdge: edge [

    edgeList append: edge

]

{ #category : #'as yet unclassified' }
AWFYControlFlowGraph >> basicBlocks [

    ^ basicBlockMap

]

{ #category : #'as yet unclassified' }
AWFYControlFlowGraph >> createNode: name [

    | node |

    (basicBlockMap at: name) notNil
      ifTrue:  [ node := basicBlockMap at: name ]
      ifFalse: [
        node := AWFYBasicBlock new: name.
        basicBlockMap at: name put: node ].
      
    self numNodes = 1 ifTrue: [startNode := node].
    ^ node 
]

{ #category : #'as yet unclassified' }
AWFYControlFlowGraph >> initialize [

    basicBlockMap := AWFYVector new.
    edgeList := AWFYVector new.

]

{ #category : #'as yet unclassified' }
AWFYControlFlowGraph >> numNodes [

    ^ basicBlockMap size

]

{ #category : #'as yet unclassified' }
AWFYControlFlowGraph >> startBasicBlock [

    ^ startNode

]
