update
removeMorphAndRestoreLayoutFrame: aMorph 

	"Same as addMorph:fullFrame, but inverts the spacing"
	self cellInset > 0 ifTrue: [ | left right bottom top spacing layoutFrame |
		layoutFrame := aMorph layoutFrame.

		spacing := self cellInset negated.
		left := layoutFrame leftOffset ifNil: [0].
		right := layoutFrame rightOffset ifNil: [0].

		bottom := layoutFrame bottomOffset ifNil: [0].
		top := layoutFrame topOffset ifNil: [0].
		
		"Add a spacing to the frame if it is not top or leftmost."
		layoutFrame leftFraction = 0
			ifFalse: [left := left + spacing]
			ifTrue: [
				"Expect another, resizable widget besides me if I have a fixed width."
				layoutFrame hasFixedWidth ifTrue: [right := right - spacing]].
		layoutFrame topFraction = 0
			ifFalse: [top := top + spacing]
			ifTrue: [
				"Expect another, resizable widget besides me if I have a fixed height."
				layoutFrame hasFixedHeight ifTrue: [bottom := bottom - spacing]].
			
		layoutFrame
			topOffset: top;
			bottomOffset: bottom;
			leftOffset: left;
			rightOffset: right].
		
	aMorph delete