Class {
	#name : #SomSet,
	#superclass : #Object,
	#instVars : [
		'items'
	],
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
SomSet class >> new [

    ^ super new initialize: 10.

]

{ #category : #'as yet unclassified' }
SomSet >> add: anObject [

    (self contains: anObject)
        ifFalse: [ items append: anObject ]

]

{ #category : #'as yet unclassified' }
SomSet >> collect: block [
 | coll |
    coll := Vector new.
    self forEach: [ :e | coll append: (block value: e) ].
    ^ coll

]

{ #category : #'as yet unclassified' }
SomSet >> contains: anObject [

    ^ self hasSome: [ :it | it = anObject ]

]

{ #category : #'as yet unclassified' }
SomSet >> forEach: block [
 items forEach: block 
]

{ #category : #'as yet unclassified' }
SomSet >> getOne: block [
 ^ items getOne:  block 
]

{ #category : #'as yet unclassified' }
SomSet >> hasSome: block [
 ^ items hasSome: block 
]

{ #category : #'as yet unclassified' }
SomSet >> initialize: size [

    items := Vector new: size.

]

{ #category : #'as yet unclassified' }
SomSet >> removeAll [
 ^ items removeAll 
]

{ #category : #'as yet unclassified' }
SomSet >> size [
 ^ items size 
]
