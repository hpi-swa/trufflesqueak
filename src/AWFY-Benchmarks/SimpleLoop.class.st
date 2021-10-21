Class {
	#name : #SimpleLoop,
	#superclass : #Object,
	#instVars : [
		'counter',
		'depthLevel',
		'parent_',
		'isRoot_',
		'nestingLevel_',
		'header',
		'isReducible',
		'basicBlocks',
		'children'
	],
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
SimpleLoop class >> basicBlock: bb reducible: isReducible [

    ^ self new init: bb reducible: isReducible

]

{ #category : #'as yet unclassified' }
SimpleLoop >> addChildLoop: loop [

    children add: loop

]

{ #category : #'as yet unclassified' }
SimpleLoop >> addNode: bb [

    basicBlocks add: bb

]

{ #category : #'as yet unclassified' }
SimpleLoop >> children [
 ^ children 
]

{ #category : #'as yet unclassified' }
SimpleLoop >> counter [
 ^ counter 
]

{ #category : #'as yet unclassified' }
SimpleLoop >> counter: val [
 counter := val 
]

{ #category : #'as yet unclassified' }
SimpleLoop >> depthLevel [
 ^ depthLevel 
]

{ #category : #'as yet unclassified' }
SimpleLoop >> depthLevel: val [
 depthLevel := val 
]

{ #category : #'as yet unclassified' }
SimpleLoop >> init: aBB reducible: aBool [

    counter    := 0.
    depthLevel := 0.

    isRoot_ := false.
    nestingLevel_ := 0.
    header  := aBB.
    isReducible := aBool.
    basicBlocks := SomIdentitySet new.
    children    := SomIdentitySet new.

    aBB notNil ifTrue: [ basicBlocks add: aBB ]

]

{ #category : #'as yet unclassified' }
SimpleLoop >> isRoot [
 ^ isRoot_ 
]

{ #category : #'as yet unclassified' }
SimpleLoop >> nestingLevel [
 ^ nestingLevel_ 
]

{ #category : #'as yet unclassified' }
SimpleLoop >> nestingLevel: level [

    nestingLevel_ := level.
    level = 0 ifTrue: [ self setIsRoot ]

]

{ #category : #'as yet unclassified' }
SimpleLoop >> parent [
 ^ parent_ 
]

{ #category : #'as yet unclassified' }
SimpleLoop >> parent: val [

    parent_ := val.
    parent_ addChildLoop: self

]

{ #category : #'as yet unclassified' }
SimpleLoop >> setIsRoot [
 isRoot_ := true 
]
