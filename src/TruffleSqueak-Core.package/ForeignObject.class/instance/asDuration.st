converting
asDuration
	(Interop isDuration: self)
		ifTrue: [ | javaDuration seconds nanos |
			javaDuration := Interop asDuration: self.
			seconds := javaDuration getSeconds.
			nanos := javaDuration getNano.
			^ Duration seconds: seconds nanoSeconds: nanos ]
		ifFalse: [ self error: 'Foreign object cannot be represented as duration' ]