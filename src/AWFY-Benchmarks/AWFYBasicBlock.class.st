Class {
	#name : #AWFYBasicBlock,
	#superclass : #Object,
	#instVars : [
		'inEdges',
		'outEdges',
		'name'
	],
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
AWFYBasicBlock class >> new: name [

    ^ self new init: name

]

{ #category : #'as yet unclassified' }
AWFYBasicBlock >> addInEdge: from [

    inEdges append: from

]

{ #category : #'as yet unclassified' }
AWFYBasicBlock >> addOutEdge: to [

    outEdges append: to

]

{ #category : #'as yet unclassified' }
AWFYBasicBlock >> customHash [

    ^ name

]

{ #category : #'as yet unclassified' }
AWFYBasicBlock >> inEdges [
 ^ inEdges 
]

{ #category : #'as yet unclassified' }
AWFYBasicBlock >> init: aName [

    inEdges  := AWFYVector new: 2.
    outEdges := AWFYVector new: 2.
    name     := aName

]

{ #category : #'as yet unclassified' }
AWFYBasicBlock >> numPred [
 ^ inEdges size 
]

{ #category : #'as yet unclassified' }
AWFYBasicBlock >> outEdges [
 ^ outEdges 
]
