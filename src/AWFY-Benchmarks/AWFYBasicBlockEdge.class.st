Class {
	#name : #AWFYBasicBlockEdge,
	#superclass : #Object,
	#instVars : [
		'from',
		'to'
	],
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
AWFYBasicBlockEdge class >> for: cfg from: fromName to: toName [

    ^ self new init: cfg from: fromName to: toName 
]

{ #category : #'as yet unclassified' }
AWFYBasicBlockEdge >> init: cfg from: fromName to: toName [

    from := cfg createNode: fromName.
    to   := cfg createNode: toName.

    from addOutEdge: to.
    to   addInEdge:  from.
    cfg  addEdge:    self

]
