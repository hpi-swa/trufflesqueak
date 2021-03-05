converting
asDate
	^ (Interop isDate: self)
		ifTrue: [ Date starting: (DateAndTime fromUnixTime:
				(Interop asDate: self) toEpochDay * 24 * 60 * 60) ]
		ifFalse: [ self error: 'Foreign object cannot be represented as date' ]