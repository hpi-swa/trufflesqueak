public
frameFractions: aRectangle

	| frame |
	frame := self layoutFrame ifNil: [LayoutFrame new].
	frame
		topFraction: aRectangle top;
		bottomFraction: aRectangle bottom;
		leftFraction: aRectangle left;
		rightFraction: aRectangle right.
	self layoutFrame: frame.
	self layoutChanged