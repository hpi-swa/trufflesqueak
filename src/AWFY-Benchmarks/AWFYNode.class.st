Class {
	#name : #AWFYNode,
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
AWFYNode class >> key: key value: value [

    ^ self new init: key value: value

]

{ #category : #'as yet unclassified' }
AWFYNode >> color [
 ^ color 
]

{ #category : #'as yet unclassified' }
AWFYNode >> color: sym [
 color := sym 
]

{ #category : #'as yet unclassified' }
AWFYNode >> init: aKey value: aValue [

    key   := aKey.
    value := aValue.
    color := #red.

]

{ #category : #'as yet unclassified' }
AWFYNode >> key [
 ^ key 
]

{ #category : #'as yet unclassified' }
AWFYNode >> left [
 ^ left 
]

{ #category : #'as yet unclassified' }
AWFYNode >> left: n [
 left := n 
]

{ #category : #'as yet unclassified' }
AWFYNode >> parent [
 ^ parent 
]

{ #category : #'as yet unclassified' }
AWFYNode >> parent: n [
 parent := n 
]

{ #category : #'as yet unclassified' }
AWFYNode >> right [
 ^ right 
]

{ #category : #'as yet unclassified' }
AWFYNode >> right: n [
 right := n 
]

{ #category : #'as yet unclassified' }
AWFYNode >> successor [

    | x y |
    x := self.
    x right notNil ifTrue: [
      ^ AWFYRedBlackTree treeMinimum: x right ].

    y := x parent.
    [ y notNil and: [ x == y right ]] whileTrue: [
      x := y.
      y := y parent ].
    ^ y

]

{ #category : #'as yet unclassified' }
AWFYNode >> value [
 ^ value 
]

{ #category : #'as yet unclassified' }
AWFYNode >> value: val [
 value := val 
]
