Class {
	#name : #AWFYListElement,
	#superclass : #Object,
	#instVars : [
		'val',
		'next'
	],
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
AWFYListElement class >> new: n [
 ^ self new initialize: n 
]

{ #category : #'as yet unclassified' }
AWFYListElement >> initialize: n [

    val  := n.
    next := nil.

]

{ #category : #'as yet unclassified' }
AWFYListElement >> length [
 next isNil ifTrue: [ ^ 1 ] ifFalse: [ ^ (1 + next length) ] 
]

{ #category : #'as yet unclassified' }
AWFYListElement >> next [
 ^ next 
]

{ #category : #'as yet unclassified' }
AWFYListElement >> next: element [
 next := element 
]

{ #category : #'as yet unclassified' }
AWFYListElement >> val [
 ^ val 
]

{ #category : #'as yet unclassified' }
AWFYListElement >> val: n [
 val := n 
]
