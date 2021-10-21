Class {
	#name : #Constants,
	#superclass : #Object,
	#classVars : [
		'horizontal',
		'vertical'
	],
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
Constants class >> GoodVoxelSize [
 "ProximityRadius *" ^ 2.0 
]

{ #category : #'as yet unclassified' }
Constants class >> MaxX [
 ^ 1000.0 
]

{ #category : #'as yet unclassified' }
Constants class >> MaxY [
 ^ 1000.0 
]

{ #category : #'as yet unclassified' }
Constants class >> MaxZ [
 ^ 10.0 
]

{ #category : #'as yet unclassified' }
Constants class >> MinX [
 ^ 0.0 
]

{ #category : #'as yet unclassified' }
Constants class >> MinY [
 ^ 0.0 
]

{ #category : #'as yet unclassified' }
Constants class >> MinZ [
 ^ 0.0 
]

{ #category : #'as yet unclassified' }
Constants class >> ProximityRadius [
 ^ 1.0 
]

{ #category : #'as yet unclassified' }
Constants class >> horizontal [
 ^ horizontal 
]

{ #category : #'as yet unclassified' }
Constants class >> initialize [

    horizontal := Vector2D x: self GoodVoxelSize y: 0.0.
    vertical   := Vector2D x: 0.0 y: self GoodVoxelSize.

]

{ #category : #'as yet unclassified' }
Constants class >> vertical [
 ^ vertical 
]
