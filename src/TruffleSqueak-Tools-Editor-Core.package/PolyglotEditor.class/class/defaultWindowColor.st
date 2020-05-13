helpers
defaultWindowColor

	^ self userInterfaceTheme color ifNil: [Color veryVeryLightGray]