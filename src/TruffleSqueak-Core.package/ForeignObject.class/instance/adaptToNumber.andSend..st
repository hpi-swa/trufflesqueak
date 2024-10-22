converting
adaptToNumber: rcvr andSend: selector
	"If I am involved in arithmetic with a number, convert me to a number."
	(Interop fitsInLong: self) ifTrue: [ ^ rcvr perform: selector with: (Interop asLong: self) ].
	(Interop fitsInDouble: self) ifTrue: [ ^ rcvr perform: selector with: (Interop asDouble: self) ].
	(Interop fitsInBigInteger: self) ifTrue: [ ^ rcvr perform: selector with: (Interop asBigInteger: self) ].
	self error: 'Cannot adapt foreign object to number'