what to show
showCallTargetInfo: aBoolean
	self contentsSymbol: (aBoolean ifFalse: [#source] ifTrue: [#callTargetInfo])