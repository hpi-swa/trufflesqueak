toolbuilder
topConstantHeightFrame: height fromLeft: leftFraction width: rightFraction
	^LayoutFrame new
		topFraction: 0 offset: 0;
		leftFraction: leftFraction offset: 0;
		rightFraction: (leftFraction + rightFraction) offset: 0;
		bottomFraction: 0 offset: height;
		yourself.