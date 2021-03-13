helpers
textColorFor: aCount with: aMax
	
	^ aCount >= 0
		ifTrue: [ | r |
			r := 1.0 * aCount / aMax.
			TextColor color: (Color
				r: r
				g: 0
				b: 1.0 - r) ]
		ifFalse: [ TextColor gray]