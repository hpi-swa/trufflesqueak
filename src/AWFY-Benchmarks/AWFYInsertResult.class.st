Class {
	#name : #AWFYInsertResult,
	#superclass : #Object,
	#instVars : [
		'isNewEntry',
		'newNode',
		'oldValue'
	],
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
AWFYInsertResult class >> new: isNewEntry node: newNode value: oldValue [

    ^ self new init: isNewEntry node: newNode value: oldValue

]

{ #category : #'as yet unclassified' }
AWFYInsertResult >> init: aBool node: aNode value: val [

    isNewEntry := aBool.
    newNode    := aNode.
    oldValue   := val.

]

{ #category : #'as yet unclassified' }
AWFYInsertResult >> isNewEntry [
 ^ isNewEntry 
]

{ #category : #'as yet unclassified' }
AWFYInsertResult >> newNode [
 ^ newNode 
]

{ #category : #'as yet unclassified' }
AWFYInsertResult >> oldValue [
 ^ oldValue 
]
