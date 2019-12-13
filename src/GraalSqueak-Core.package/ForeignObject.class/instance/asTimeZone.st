converting
asTimeZone
	(Interop isTimeZone: self)
		ifTrue: [ | javaZoneId fullStyle shortStyle locale now offset name abbreviation |
			javaZoneId := Interop asTimeZone: self.
			fullStyle := (Java type: 'java.time.format.TextStyle') FULL.
			shortStyle := (Java type: 'java.time.format.TextStyle') SHORT.
			locale := (Java type: 'java.util.Locale') ENGLISH.
			now := (Java type: 'java.time.Instant') now.
			offset := Duration seconds: (javaZoneId getRules getOffset: now) getTotalSeconds.
			name := javaZoneId getDisplayName: fullStyle _: locale.
			abbreviation := javaZoneId getDisplayName: shortStyle _: locale.
			^ TimeZone offset: offset name: name abbreviation: abbreviation.]
		ifFalse: [ self error: 'Foreign object cannot be represented as time zone' ]