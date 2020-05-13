path utils
directorySeparatorsUsedIn: aString

	^ (aString indexOf: $/) > 0 ifTrue: ['/'] ifFalse: ['\']