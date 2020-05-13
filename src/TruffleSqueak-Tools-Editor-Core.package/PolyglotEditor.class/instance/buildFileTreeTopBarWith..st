building
buildFileTreeTopBarWith: aBuilder

	^ aBuilder pluggablePanelSpec new
		layout: #horizontal;
		children: {
			aBuilder pluggableActionButtonSpec new
				label: 'Choose new root directory...';
				action: [self chooseNewRootDirectory];
				model: self;
				yourself.
		};
		frame: (LayoutFrame new leftFraction: 0 offset: 0;
					 topFraction: 0 offset: 0;
					 rightFraction: 1 offset: 0;
					 bottomFraction: 0 offset: 25)
		yourself