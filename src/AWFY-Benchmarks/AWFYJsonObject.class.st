Class {
	#name : #AWFYJsonObject,
	#superclass : #AWFYJsonValue,
	#instVars : [
		'names',
		'values',
		'table'
	],
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
AWFYJsonObject class >> fromAssociations: collectionOfAssociations [

	| result |
	result := self new.
	
	collectionOfAssociations do: [:each |
		result at: each key put: each value ].
	^ result
]

{ #category : #'as yet unclassified' }
AWFYJsonObject class >> new [
 ^ super new initialize 
]

{ #category : #'as yet unclassified' }
AWFYJsonObject class >> readFrom: string [

    ^ (AWFYJsonValue readFrom: string) asObject

]

{ #category : #'as yet unclassified' }
AWFYJsonObject >> add: name with: aJsonValue [

    name ifNil: [ self error: 'name is null'  ].
    aJsonValue ifNil: [ self error: 'aJsonValue is null' ].

    table at: name put: names size + 1. "+ 1 for 1-based indexing"
    names  append: name.
    values append: aJsonValue.

]

{ #category : #'as yet unclassified' }
AWFYJsonObject >> asObject [
 ^ self 
]

{ #category : #'as yet unclassified' }
AWFYJsonObject >> at: name [

    | idx |
    name ifNil: [ self error: 'name is null'  ].
    idx := self indexOf: name.
    idx = 0
      ifTrue:  [ ^ nil ]
      ifFalse: [ ^ values at: idx ]

]

{ #category : #'as yet unclassified' }
AWFYJsonObject >> indexOf: name [

    | idx |
    idx := table at: name.    
    (idx ~= 0 and: [name = (names at: idx)]) ifTrue: [ ^ idx ].
    ^ self error: 'not implement'

]

{ #category : #'as yet unclassified' }
AWFYJsonObject >> initialize [

    names  := AWFYVector new.
    values := AWFYVector new.
    table  := AWFYHashIndexTable new

]

{ #category : #'as yet unclassified' }
AWFYJsonObject >> isEmpty [

    ^ names isEmpty

]

{ #category : #'as yet unclassified' }
AWFYJsonObject >> isObject [
 ^ true 
]

{ #category : #'as yet unclassified' }
AWFYJsonObject >> size [

    ^ names size

]
