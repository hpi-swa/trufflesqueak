Class {
	#name : #AWFYRbtEntry,
	#superclass : #Object,
	#instVars : [
		'key',
		'value'
	],
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
AWFYRbtEntry class >> key: key value: value [

    ^ self new init: key value: value

]

{ #category : #'as yet unclassified' }
AWFYRbtEntry >> init: aKey value: val [

    key   := aKey.
    value := val.

]

{ #category : #'as yet unclassified' }
AWFYRbtEntry >> key [
 ^ key 
]

{ #category : #'as yet unclassified' }
AWFYRbtEntry >> value [
 ^ value 
]
