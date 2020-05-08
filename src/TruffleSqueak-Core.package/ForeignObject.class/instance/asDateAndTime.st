converting
asDateAndTime
	(Interop isInstant: self)
		ifTrue: [ ^ (DateAndTime fromUnixTime: (Interop asInstant: self) getEpochSecond) asLocal ]
		ifFalse: [ self error: 'Foreign object cannot be represented as date and time' ]