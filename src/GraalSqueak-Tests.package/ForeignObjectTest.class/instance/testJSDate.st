testing
testJSDate
	| jsDate dateAndTime |
	(Polyglot isPermitted: Polyglot languageIdJS) ifFalse: [ ^self ].

	jsDate := (Polyglot eval: Polyglot languageIdJS string: 'new Date(949322096000)').
	
	self assert: (Interop isInstant: jsDate).
	dateAndTime := DateAndTime fromUnixTime: 949322096.
	self assert: dateAndTime equals: jsDate asDateAndTime.

	self assert: (Interop isDate: jsDate).
	self assert: dateAndTime asDate equals: jsDate asDate.
	
	self assert: (Interop isTime: jsDate).
	self assert: dateAndTime asTime equals: jsDate asTime.

	self assert: (Interop isTimeZone: jsDate).
	self assert: jsDate asTimeZone abbreviation notEmpty.

	self deny: (Interop isDuration: jsDate)
