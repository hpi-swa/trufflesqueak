private
byteTo2DigitHex: aNumber

	| string | 
	string := aNumber printStringBase: 16.
	string size < 2 ifTrue: [string := '0', string].
	^ string