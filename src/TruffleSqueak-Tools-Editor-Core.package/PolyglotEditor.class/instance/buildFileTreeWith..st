building
buildFileTreeWith: aBuilder

	^ aBuilder pluggablePanelSpec new
		frame: (LayoutFrame new
			leftFraction: 0 offset: 0;
			topFraction: 0 offset: 0;
			rightFraction: 0.25 offset: 0;
			bottomFraction: 1 offset: 0);
		children: {
			PEFileExplorerSpec new
				model: self;
				getRootDirectories: #rootDirectories;
				setSelectedFileEntry: #openFile:;
				frame:  (LayoutFrame new
					leftFraction: 0 offset: 0;
					topFraction: 0 offset: 25;
					rightFraction: 1 offset: 0;
					bottomFraction: 1 offset: 0);
				yourself.
		 self buildFileTreeTopBarWith: aBuilder}