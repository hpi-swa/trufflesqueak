public
supports: aFileEntry

	Smalltalk at: #MMBrowserMorph ifAbsent: [^ -100].
	^ (#(gif jpeg jpg png mp4 mp3 svg pdf wav ogg) includes: aFileEntry extension asLowercase) ifTrue: [5] ifFalse: [0]