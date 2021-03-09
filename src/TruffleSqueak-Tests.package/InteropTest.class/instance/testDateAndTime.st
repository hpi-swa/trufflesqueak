testing
testDateAndTime
	| now localTimeZone |
	Java primitiveIsHostLookupAllowed ifFalse: [ ^self ].
	
	self deny: (Interop isDate: Smalltalk).
	self assert: (Interop isDate: Date today).
	self assert: Date today equals: (Interop asDate: Date today) asDate.

	now := Time now.
	self deny: (Interop isTime: Smalltalk).
	self assert: (Interop isTime: now).
	self assert: now equals: (Interop asTime: now) asTime.
	
	self deny: (Interop isTimeZone: Smalltalk).
	self assert: (Interop isTimeZone: TimeZone default).
	self assert: TimeZone default abbreviation
		equals: (Interop asTimeZone: TimeZone default) asTimeZone abbreviation.
	
	now := DateAndTime now.
	self assert: (Interop isTime: now).
	self assert: now asTime equals: (Interop asTime: now) asTime.
	self assert: (Interop isDate: now).
	self assert: now asDate makeUTC equals: (Interop asDate: now) asDate.
	self assert: (Interop isTimeZone:now).
	localTimeZone := DateAndTime localTimeZone.
	[
		DateAndTime localTimeZone: TimeZone timeZones first.
		self assert: now timeZoneAbbreviation
			equals: (Interop asTimeZone: now) asTimeZone abbreviation
	] ensure: [ DateAndTime localTimeZone: localTimeZone ]
