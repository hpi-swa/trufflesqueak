building
buildEditingAreaWith: aBuilder

	^  aBuilder pluggablePanelSpec new
		frame:  (LayoutFrame new
			leftFraction: 0.25 offset: 0;
			topFraction: 0 offset: 0;
			rightFraction: 1 offset: 0;
			bottomFraction: 1 offset: 0);
		children: {
			self buildEditingBottomBarWith: aBuilder.
			PEPluggablePanelSpec new
				wantsResizeHandles: true;
				frame: (LayoutFrame new
					leftFraction: 0 offset: 0;
					topFraction: 0 offset: 25;
					rightFraction: 1 offset: 0;
					bottomFraction: 1 offset: self bottomBarHeight negated);
				model: self;
				children: #panes;
				yourself.
			self buildEditingTopBarWith: aBuilder}