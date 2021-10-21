Class {
	#name : #AWFYSomSet,
	#superclass : #Object,
	#instVars : [
		'items'
	],
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
AWFYSomSet class >> new [

    ^ super new initialize: 10.

]

{ #category : #'as yet unclassified' }
AWFYSomSet >> add: anObject [

    (self contains: anObject)
        ifFalse: [ items append: anObject ]

]

{ #category : #'as yet unclassified' }
AWFYSomSet >> collect: block [
 | coll |
    coll := AWFYVector new.
    self forEach: [ :e | coll append: (block value: e) ].
    ^ coll

]

{ #category : #'as yet unclassified' }
AWFYSomSet >> contains: anObject [

    ^ self hasSome: [ :it | it = anObject ]

]

{ #category : #'as yet unclassified' }
AWFYSomSet >> forEach: block [
 items forEach: block 
]

{ #category : #'as yet unclassified' }
AWFYSomSet >> getOne: block [
 ^ items getOne:  block 
]

{ #category : #'as yet unclassified' }
AWFYSomSet >> hasSome: block [
 ^ items hasSome: block 
]

{ #category : #'as yet unclassified' }
AWFYSomSet >> initialize: size [

    items := AWFYVector new: size.

]

{ #category : #'as yet unclassified' }
AWFYSomSet >> removeAll [
 ^ items removeAll 
]

{ #category : #'as yet unclassified' }
AWFYSomSet >> size [
 ^ items size 
]
