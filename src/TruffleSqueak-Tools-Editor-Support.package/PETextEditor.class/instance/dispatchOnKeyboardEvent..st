typing support
dispatchOnKeyboardEvent: aKeyboardEvent 

	aKeyboardEvent keyCharacter == Character cr ifTrue: [
		self clearParens.
		self addString: (String streamContents: [:characterStream | 
			characterStream
				nextPutAll: self lineEndings;
				tab: self tabCount]).
		^ false].
	
	^ super dispatchOnKeyboardEvent: aKeyboardEvent