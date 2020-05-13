public
lineEndingsName

	^ (#(multiple unknown) includes: lineEndingsSymbol) 
		ifFalse: [lineEndingsSymbol asUppercase]
		ifTrue: [lineEndingsSymbol]