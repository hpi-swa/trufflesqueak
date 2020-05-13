private
toTextColor: aString

	| hex |
	hex := aString.
	hex size = 4 ifTrue: [
		hex := '#', (hex at: 2), (hex at: 2), (hex at: 3), (hex at: 3), (hex at: 4), (hex at: 4)].
	^ TextColor color: (Color fromString: hex) 