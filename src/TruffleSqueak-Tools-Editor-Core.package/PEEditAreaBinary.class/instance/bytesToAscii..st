private
bytesToAscii: aCollection

	^ (String new: aCollection size) replaceFrom: 1 to: aCollection size with: (aCollection collect: [:each | | character | 
		character := each asCharacter.
		(character isAlphaNumeric and: [character isAscii]) ifTrue: [character] ifFalse: [$.]])