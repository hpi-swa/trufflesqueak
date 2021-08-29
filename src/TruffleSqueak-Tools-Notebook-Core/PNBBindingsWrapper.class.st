Class {
	#name : #PNBBindingsWrapper,
	#superclass : #ObjectExplorerWrapper,
	#category : 'TruffleSqueak-Tools-Notebook-Core'
}

{ #category : #converting }
PNBBindingsWrapper >> asString [
	| label separator |
	label := self itemName ifNil: [''].
	(label includes: Character cr)
		ifTrue: [label := label withSeparatorsCompacted].
	 
	separator := self class showContentsInColumns
		ifTrue: [String tab]
		ifFalse: [label ifEmpty: [''] ifNotEmpty: [': ']].

	^ '{1}{2}{3} members in total' format: {label.separator.Interop getMemberSize: self object}
]

{ #category : #accessing }
PNBBindingsWrapper >> contents [
	^ (Interop getMembers: self object) sorted collect: [:member |
		PolyglotObjectExplorerWrapper
			with: self object
			member: member]
]
