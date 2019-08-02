converting
adaptToNumber: rcvr andSend: selector
	"If I am involved in arithmetic with a number, convert me to a number."
	self primitiveFitsInLong ifTrue: [ ^ rcvr perform: selector with: self primitiveAsLong ].
	self primitiveFitsInDouble ifTrue: [ ^ rcvr perform: selector with: self primitiveAsDouble ].
	self error: 'Cannot adapt foreign object to number'