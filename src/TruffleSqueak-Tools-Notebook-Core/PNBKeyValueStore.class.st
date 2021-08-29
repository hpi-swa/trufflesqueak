Class {
	#name : #PNBKeyValueStore,
	#superclass : #Object,
	#instVars : [
		'dictionary'
	],
	#category : #'TruffleSqueak-Tools-Notebook-Core'
}

{ #category : #accessing }
PNBKeyValueStore >> at: key [
	^ self dictionary at: key
]

{ #category : #accessing }
PNBKeyValueStore >> at: key put: anObject [
	^ self dictionary at: key put: anObject
]

{ #category : #accessing }
PNBKeyValueStore >> dictionary [

	^ dictionary ifNil: [ dictionary := Dictionary new ]
]

{ #category : #overrides }
PNBKeyValueStore >> interopGetMembers: includeInternal [
	^ self dictionary keys
]

{ #category : #overrides }
PNBKeyValueStore >> interopIsMemberInsertable: member [
	^ member isString and: [ (self interopIsMemberReadable: member) not ]
]

{ #category : #overrides }
PNBKeyValueStore >> interopIsMemberInvocable: member [
	^ false
]

{ #category : #overrides }
PNBKeyValueStore >> interopIsMemberModifiable: member [
	^ self interopIsMemberReadable: member
]

{ #category : #overrides }
PNBKeyValueStore >> interopIsMemberReadable: member [
	^ member isString and: [ self dictionary includesKey: member asString ]
]

{ #category : #overrides }
PNBKeyValueStore >> interopIsMemberRemovable: member [
	^ self interopIsMemberReadable: member
]

{ #category : #overrides }
PNBKeyValueStore >> interopReadMember: member [
	^ self dictionary at: member asString
]

{ #category : #overrides }
PNBKeyValueStore >> interopRemoveMember: member [
	^ self dictionary removeKey: member asString ifAbsent: nil 
]

{ #category : #overrides }
PNBKeyValueStore >> interopWriteMember: member and: value [
	^ self dictionary at: member asString put: value
]
