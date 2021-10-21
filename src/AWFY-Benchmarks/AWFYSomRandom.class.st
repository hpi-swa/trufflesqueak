Class {
	#name : #AWFYSomRandom,
	#superclass : #Object,
	#instVars : [
		'seed'
	],
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
AWFYSomRandom class >> new [
 ^ super new initialize 
]

{ #category : #'as yet unclassified' }
AWFYSomRandom >> initialize [
 seed := 74755 
]

{ #category : #'as yet unclassified' }
AWFYSomRandom >> next [

    seed := ((seed * 1309) + 13849) bitAnd: 65535.
    ^ seed

]
