requests
correctFrom: start to: stop with: aString
	
	| loc |
	aString = '#insert period' ifTrue:
		[loc := start.
		[(loc := loc-1)>0 and: [(self sourceCode at: loc) isSeparator]]
			whileTrue: [loc := loc-1].
		^ self correctFrom: loc+1 to: loc with: '.'].
	
	self currentSource: (self sourceCode copyReplaceFrom: start to: stop with: aString)