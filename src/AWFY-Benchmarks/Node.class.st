Class {
	#name : #Node,
	#superclass : #Object,
	#instVars : [
		'key',
		'value',
		'left',
		'right',
		'parent',
		'color'
	],
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
Node class >> key: key value: value [

    ^ self new init: key value: value

]

{ #category : #'as yet unclassified' }
Node >> color [
 ^ color 
]

{ #category : #'as yet unclassified' }
Node >> color: sym [
 color := sym 
]

{ #category : #'as yet unclassified' }
Node >> init: aKey value: aValue [

    key   := aKey.
    value := aValue.
    color := #red.

]

{ #category : #'as yet unclassified' }
Node >> key [
 ^ key 
]

{ #category : #'as yet unclassified' }
Node >> left [
 ^ left 
]

{ #category : #'as yet unclassified' }
Node >> left: n [
 left := n 
]

{ #category : #'as yet unclassified' }
Node >> parent [
 ^ parent 
]

{ #category : #'as yet unclassified' }
Node >> parent: n [
 parent := n 
]

{ #category : #'as yet unclassified' }
Node >> right [
 ^ right 
]

{ #category : #'as yet unclassified' }
Node >> right: n [
 right := n 
]

{ #category : #'as yet unclassified' }
Node >> successor [

    | x y |
    x := self.
    x right notNil ifTrue: [
      ^ RedBlackTree treeMinimum: x right ].

    y := x parent.
    [ y notNil and: [ x == y right ]] whileTrue: [
      x := y.
      y := y parent ].
    ^ y

]

{ #category : #'as yet unclassified' }
Node >> value [
 ^ value 
]

{ #category : #'as yet unclassified' }
Node >> value: val [
 value := val 
]
