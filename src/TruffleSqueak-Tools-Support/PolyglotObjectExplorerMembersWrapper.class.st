Class {
	#name : #PolyglotObjectExplorerMembersWrapper,
	#superclass : #ObjectExplorerWrapper,
	#instVars : [
		'members'
	],
	#category : 'TruffleSqueak-Tools-Support'
}

{ #category : #'instance creation' }
PolyglotObjectExplorerMembersWrapper class >> with: anObject name: aString model: aModel parent: aParent members: members [

	^self new 
		setItem: anObject name: aString model: aModel parent: aParent;
		members: members

]

{ #category : #converting }
PolyglotObjectExplorerMembersWrapper >> asString [
	| label separator |
	label := self itemName ifNil: [''].
	(label includes: Character cr)
		ifTrue: [label := label withSeparatorsCompacted].
	 
	separator := self class showContentsInColumns
		ifTrue: [String tab]
		ifFalse: [label ifEmpty: [''] ifNotEmpty: [': ']].

	^ '{1}{2}{3} in total' format: {label. separator. self members size}
]

{ #category : #accessing }
PolyglotObjectExplorerMembersWrapper >> contents [
	^ self members collect: [:member |
		PolyglotObjectExplorerWrapper
			with: self object
			member: member]
]

{ #category : #accessing }
PolyglotObjectExplorerMembersWrapper >> icon [
	^ ToolIcons iconNamed: #blank
]

{ #category : #accessing }
PolyglotObjectExplorerMembersWrapper >> members [

	^ members
]

{ #category : #accessing }
PolyglotObjectExplorerMembersWrapper >> members: anObject [

	members := anObject.
]
