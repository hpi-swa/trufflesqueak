querying
forExtension: extension ifNone: aBlock

	^ self all detect: [:each | each extensions includes: extension] ifNone: aBlock