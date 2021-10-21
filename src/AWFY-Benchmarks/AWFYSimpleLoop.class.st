Class {
	#name : #AWFYSimpleLoop,
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
AWFYSimpleLoop class >> basicBlock: bb reducible: isReducible [

    ^ self new init: bb reducible: isReducible

]

{ #category : #'as yet unclassified' }
AWFYSimpleLoop >> addChildLoop: loop [

    children add: loop

]

{ #category : #'as yet unclassified' }
AWFYSimpleLoop >> addNode: bb [

    basicBlocks add: bb

]

{ #category : #'as yet unclassified' }
AWFYSimpleLoop >> children [
 ^ children 
]

{ #category : #'as yet unclassified' }
AWFYSimpleLoop >> counter [
 ^ counter 
]

{ #category : #'as yet unclassified' }
AWFYSimpleLoop >> counter: val [
 counter := val 
]

{ #category : #'as yet unclassified' }
AWFYSimpleLoop >> depthLevel [
 ^ depthLevel 
]

{ #category : #'as yet unclassified' }
AWFYSimpleLoop >> depthLevel: val [
 depthLevel := val 
]

{ #category : #'as yet unclassified' }
AWFYSimpleLoop >> init: aBB reducible: aBool [

    counter    := 0.
    depthLevel := 0.

    isRoot_ := false.
    nestingLevel_ := 0.
    header  := aBB.
    isReducible := aBool.
    basicBlocks := AWFYSomIdentitySet new.
    children    := AWFYSomIdentitySet new.

    aBB notNil ifTrue: [ basicBlocks add: aBB ]

]

{ #category : #'as yet unclassified' }
AWFYSimpleLoop >> isRoot [
 ^ isRoot_ 
]

{ #category : #'as yet unclassified' }
AWFYSimpleLoop >> nestingLevel [
 ^ nestingLevel_ 
]

{ #category : #'as yet unclassified' }
AWFYSimpleLoop >> nestingLevel: level [

    nestingLevel_ := level.
    level = 0 ifTrue: [ self setIsRoot ]

]

{ #category : #'as yet unclassified' }
AWFYSimpleLoop >> parent [
 ^ parent_ 
]

{ #category : #'as yet unclassified' }
AWFYSimpleLoop >> parent: val [

    parent_ := val.
    parent_ addChildLoop: self

]

{ #category : #'as yet unclassified' }
AWFYSimpleLoop >> setIsRoot [
 isRoot_ := true 
]
