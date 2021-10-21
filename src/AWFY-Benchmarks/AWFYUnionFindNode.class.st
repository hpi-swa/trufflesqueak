Class {
	#name : #AWFYUnionFindNode,
	#superclass : #Object,
	#instVars : [
		'parent_',
		'bb_',
		'dfsNumber_',
		'loop'
	],
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
AWFYUnionFindNode class >> new [
 ^ super new initialize 
]

{ #category : #'as yet unclassified' }
AWFYUnionFindNode >> bb [
 ^ bb_ 
]

{ #category : #'as yet unclassified' }
AWFYUnionFindNode >> dfsNumber [
 ^ dfsNumber_ 
]

{ #category : #'as yet unclassified' }
AWFYUnionFindNode >> findSet [

    | nodeList node |
    nodeList := AWFYVector new.
    
    node := self.
    
    [node ~= node parent] whileTrue: [
      ((node parent) ~= (node parent parent)) ifTrue: [
        nodeList append: node ].
      node := node parent ].
  
    nodeList forEach: [:iter | iter union: parent_ ].
    ^ node

]

{ #category : #'as yet unclassified' }
AWFYUnionFindNode >> initNode: bb dfs: dfsNumber [

    parent_ := self.
    bb_ := bb.
    dfsNumber_ := dfsNumber.

]

{ #category : #'as yet unclassified' }
AWFYUnionFindNode >> initialize [

    dfsNumber_ := 0.

]

{ #category : #'as yet unclassified' }
AWFYUnionFindNode >> loop [
 ^ loop 
]

{ #category : #'as yet unclassified' }
AWFYUnionFindNode >> loop: aLoop [
 loop := aLoop 
]

{ #category : #'as yet unclassified' }
AWFYUnionFindNode >> parent [
 ^ parent_ 
]

{ #category : #'as yet unclassified' }
AWFYUnionFindNode >> union: basicBlock [

    parent_ := basicBlock

]
