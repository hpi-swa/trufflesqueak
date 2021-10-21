Class {
	#name : #SomRandom,
	#superclass : #Object,
	#instVars : [
		'seed'
	],
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
SomRandom class >> new [
 ^ super new initialize 
]

{ #category : #'as yet unclassified' }
SomRandom >> initialize [
 seed := 74755 
]

{ #category : #'as yet unclassified' }
SomRandom >> next [

    seed := ((seed * 1309) + 13849) bitAnd: 65535.
    ^ seed

]
