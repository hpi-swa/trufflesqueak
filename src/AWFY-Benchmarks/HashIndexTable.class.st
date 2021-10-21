Class {
	#name : #HashIndexTable,
	#superclass : #Object,
	#instVars : [
		'hashTable'
	],
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
HashIndexTable class >> new [
 ^ super new initialize 
]

{ #category : #'as yet unclassified' }
HashIndexTable >> at: name [

    | slot |
    slot := self hashSlotFor: name.

    " subtract 1, 0 stands for empty "
    ^ ((hashTable at: slot) & 255) - 1

]

{ #category : #'as yet unclassified' }
HashIndexTable >> at: name put: index [

    | slot |
    slot := self hashSlotFor: name.
    
    index < 255
      ifTrue:  [ hashTable at: slot put: index + 1 ]
      ifFalse: [ hashTable at: slot put: 0 ]

]

{ #category : #'as yet unclassified' }
HashIndexTable >> hashSlotFor: element [

    ^ ((self stringHash: element) & (hashTable size - 1)) + 1

]

{ #category : #'as yet unclassified' }
HashIndexTable >> initialize [

    hashTable := Array new: 32 withAll: 0

]

{ #category : #'as yet unclassified' }
HashIndexTable >> stringHash: s [

    "this is not a proper hash, but sufficient for the benchmark,
     and very portable!"
     ^ s size * 1402589

]
