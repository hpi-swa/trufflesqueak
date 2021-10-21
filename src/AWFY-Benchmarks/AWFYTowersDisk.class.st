Class {
	#name : #AWFYTowersDisk,
	#superclass : #Object,
	#instVars : [
		'size',
		'next'
	],
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
AWFYTowersDisk class >> new: size [
 ^ super new initialize: size 
]

{ #category : #'as yet unclassified' }
AWFYTowersDisk >> initialize: anInt [

    size := anInt

]

{ #category : #'as yet unclassified' }
AWFYTowersDisk >> next [
 ^ next 
]

{ #category : #'as yet unclassified' }
AWFYTowersDisk >> next: value [
 next := value 
]

{ #category : #'as yet unclassified' }
AWFYTowersDisk >> size [
 ^ size 
]
