path utils
baseNameOf: aString

	^ aString copyFrom: (aString lastIndexOfAnyOf: '/\') + 1 to: aString size