Class {
	#name : #Sym,
	#superclass : #Object,
	#instVars : [
		'hash'
	],
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
Sym class >> new: aHash [

    ^ self new init: aHash

]

{ #category : #'as yet unclassified' }
Sym >> customHash [
 ^ hash 
]

{ #category : #'as yet unclassified' }
Sym >> init: aHash [
 hash := aHash 
]
