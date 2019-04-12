converting
adaptToNumber: rcvr andSend: selector
	"If I am involved in arithmetic with a number, convert me to a number."
	| number |
	self fitsInLong ifTrue: [ number := self asLong ].
	self fitsInDouble ifTrue: [ number := self asDouble ].
	self isString ifTrue: [ | string |
		string := self asString.
		string size = 1 ifTrue: [	number := string asCharacter asNumber ] ].
	number ifNil: [ self error: 'Cannot adapt foreign object to number' ].

	^ rcvr perform: selector with: number