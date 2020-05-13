building
buildEditingBottomBarWith: aBuilder

	^ aBuilder pluggablePanelSpec new
		children: {
			aBuilder pluggableButtonSpec new
				model: self;
				frame: (0@0 corner: 0.2@1);
				label: #bottomBarLineEndings;
				help: 'Line Endings';
				action: #convertLineEndings;
				yourself.
			PEPluggableLabelSpec new
				label: #bottomBarLastModified;
				model: self;
				help: 'Last saved at';
				frame: (0.2@0 corner: 0.6@1)
				yourself.
			PEPluggableLabelSpec new
				label: #bottomBarLanguage;
				model: self;
				help: 'Current language';
				frame: (0.6@0 corner: 1@1)
				yourself};
		frame:  (LayoutFrame new
					leftFraction: 0 offset: 0;
					topFraction: 1 offset: self bottomBarHeight negated;
					rightFraction: 1 offset: 0;
					bottomFraction: 1 offset: 0);
		yourself