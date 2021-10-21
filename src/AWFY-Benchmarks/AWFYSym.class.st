Class {
	#name : #AWFYSym,
	#superclass : #Object,
	#instVars : [
		'hash'
	],
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
AWFYSym class >> new: aHash [

    ^ self new init: aHash

]

{ #category : #'as yet unclassified' }
AWFYSym >> customHash [
 ^ hash 
]

{ #category : #'as yet unclassified' }
AWFYSym >> init: aHash [
 hash := aHash 
]
