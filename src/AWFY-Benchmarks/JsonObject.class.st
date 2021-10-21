Class {
	#name : #JsonObject,
	#superclass : #JsonValue,
	#instVars : [
		'names',
		'values',
		'table'
	],
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
JsonObject class >> fromAssociations: collectionOfAssociations [

	| result |
	result := self new.
	
	collectionOfAssociations do: [:each |
		result at: each key put: each value ].
	^ result
]

{ #category : #'as yet unclassified' }
JsonObject class >> new [
 ^ super new initialize 
]

{ #category : #'as yet unclassified' }
JsonObject class >> readFrom: string [

    ^ (JsonValue readFrom: string) asObject

]

{ #category : #'as yet unclassified' }
JsonObject >> add: name with: aJsonValue [

    name ifNil: [ self error: 'name is null'  ].
    aJsonValue ifNil: [ self error: 'aJsonValue is null' ].

    table at: name put: names size + 1. "+ 1 for 1-based indexing"
    names  append: name.
    values append: aJsonValue.

]

{ #category : #'as yet unclassified' }
JsonObject >> asObject [
 ^ self 
]

{ #category : #'as yet unclassified' }
JsonObject >> at: name [

    | idx |
    name ifNil: [ self error: 'name is null'  ].
    idx := self indexOf: name.
    idx = 0
      ifTrue:  [ ^ nil ]
      ifFalse: [ ^ values at: idx ]

]

{ #category : #'as yet unclassified' }
JsonObject >> indexOf: name [

    | idx |
    idx := table at: name.    
    (idx ~= 0 and: [name = (names at: idx)]) ifTrue: [ ^ idx ].
    ^ self error: 'not implement'

]

{ #category : #'as yet unclassified' }
JsonObject >> initialize [

    names  := Vector new.
    values := Vector new.
    table  := HashIndexTable new

]

{ #category : #'as yet unclassified' }
JsonObject >> isEmpty [

    ^ names isEmpty

]

{ #category : #'as yet unclassified' }
JsonObject >> isObject [
 ^ true 
]

{ #category : #'as yet unclassified' }
JsonObject >> size [

    ^ names size

]
