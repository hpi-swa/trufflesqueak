interop
asCharacterOrString
	| string |
	string := self asString.
	^ string size = 1 ifTrue: [ string asCharacter ] ifFalse: [ string ]