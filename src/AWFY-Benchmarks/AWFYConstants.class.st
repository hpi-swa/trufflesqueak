Class {
	#name : #AWFYConstants,
	#superclass : #Object,
	#classVars : [
		'Horizontal',
		'Vertical'
	],
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
AWFYConstants class >> GoodVoxelSize [
 "ProximityRadius *" ^ 2.0 
]

{ #category : #'as yet unclassified' }
AWFYConstants class >> MaxX [
 ^ 1000.0 
]

{ #category : #'as yet unclassified' }
AWFYConstants class >> MaxY [
 ^ 1000.0 
]

{ #category : #'as yet unclassified' }
AWFYConstants class >> MaxZ [
 ^ 10.0 
]

{ #category : #'as yet unclassified' }
AWFYConstants class >> MinX [
 ^ 0.0 
]

{ #category : #'as yet unclassified' }
AWFYConstants class >> MinY [
 ^ 0.0 
]

{ #category : #'as yet unclassified' }
AWFYConstants class >> MinZ [
 ^ 0.0 
]

{ #category : #'as yet unclassified' }
AWFYConstants class >> ProximityRadius [
 ^ 1.0 
]

{ #category : #'as yet unclassified' }
AWFYConstants class >> horizontal [
 ^ Horizontal 
]

{ #category : #'as yet unclassified' }
AWFYConstants class >> initialize [

    Horizontal := AWFYVector2D x: self GoodVoxelSize y: 0.0.
    Vertical   := AWFYVector2D x: 0.0 y: self GoodVoxelSize.

]

{ #category : #'as yet unclassified' }
AWFYConstants class >> vertical [
 ^ Vertical 
]
