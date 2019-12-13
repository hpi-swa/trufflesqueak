converting
asTime
	(Interop isTime: self)
		ifTrue: [ | javaLocalTime seconds nanos |
			javaLocalTime := Interop asTime: self.
			seconds := javaLocalTime toSecondOfDay.
			nanos := javaLocalTime getNano.
			(Interop isTimeZone: self)
				ifTrue: [ seconds := seconds - self asTimeZone offset asSeconds ].
			^ Time seconds: seconds nanoSeconds: nanos ]
		ifFalse: [ self error: 'Foreign object cannot be represented as time' ]