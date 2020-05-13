public
supports: aFileEntry

	^ (#(gif jpeg jpg png) includes: aFileEntry extension asLowercase) ifTrue: [10] ifFalse: [0]