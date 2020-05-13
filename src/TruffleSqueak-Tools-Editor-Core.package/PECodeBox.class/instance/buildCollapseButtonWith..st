building
buildCollapseButtonWith: aBuilder

	^{aBuilder pluggableButtonSpec new
		model: self;
		label: '^';
		action: #collapseCodeBox;
		verticalResizing: #spaceFill;
		horizontalResizing: #rigid;
		yourself}