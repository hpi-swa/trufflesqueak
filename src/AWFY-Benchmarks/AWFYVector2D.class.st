Class {
	#name : #AWFYVector2D,
	#superclass : #Object,
	#instVars : [
		'x',
		'y'
	],
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
AWFYVector2D class >> x: anX y: aY [

    ^ self new initX: anX y: aY

]

{ #category : #'as yet unclassified' }
AWFYVector2D >> compare: a and: b [

    a = b ifTrue: [ ^  0 ].
    a < b ifTrue: [ ^ -1 ].
    a > b ifTrue: [ ^  1 ].

    "We say that NaN is smaller than non-NaN."
    a = a ifTrue: [ ^ 1 ].
    ^ -1

]

{ #category : #'as yet unclassified' }
AWFYVector2D >> compareTo: other [

    | result |
    result := self compare: x and: other x.
    result ~= 0 ifTrue: [ ^ result ].
    ^ self compare: y and: other y

]

{ #category : #'as yet unclassified' }
AWFYVector2D >> initX: anX y: aY [

    x := anX.
    y := aY

]

{ #category : #'as yet unclassified' }
AWFYVector2D >> minus: other [

    ^ AWFYVector2D x: x - other x
               y: y - other y

]

{ #category : #'as yet unclassified' }
AWFYVector2D >> plus: other [

    ^ AWFYVector2D x: x + other x
               y: y + other y

]

{ #category : #'as yet unclassified' }
AWFYVector2D >> x [
 ^ x 
]

{ #category : #'as yet unclassified' }
AWFYVector2D >> y [
 ^ y 
]
