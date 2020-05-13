building
buildWith: aBuilder

	| window |
	window := aBuilder build: (aBuilder pluggableWindowSpec new
		model: self;
		label: 'Polyglot Editor';
		extent: self initialEditorExtent;
		children: {
			aBuilder pluggablePanelSpec new
				wantsResizeHandles: true;
				frame: (LayoutFrame new leftFraction: 0 offset: 0;
					 topFraction: 0 offset: 0;
					 rightFraction: 1 offset: 0;
					 bottomFraction: 1 offset: 0);
				children: {
					self buildFileTreeWith: aBuilder. 
					self buildEditingAreaWith: aBuilder};
				yourself};
		yourself).
	
	^ window