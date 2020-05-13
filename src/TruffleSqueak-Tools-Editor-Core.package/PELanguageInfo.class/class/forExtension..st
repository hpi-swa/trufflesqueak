querying
forExtension: extension

	^ self forExtension: extension ifNone: [self error]