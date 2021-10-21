Class {
	#name : #TowersDisk,
	#superclass : #Object,
	#instVars : [
		'size',
		'next'
	],
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
TowersDisk class >> new: size [
 ^ super new initialize: size 
]

{ #category : #'as yet unclassified' }
TowersDisk >> initialize: anInt [

    size := anInt

]

{ #category : #'as yet unclassified' }
TowersDisk >> next [
 ^ next 
]

{ #category : #'as yet unclassified' }
TowersDisk >> next: value [
 next := value 
]

{ #category : #'as yet unclassified' }
TowersDisk >> size [
 ^ size 
]
