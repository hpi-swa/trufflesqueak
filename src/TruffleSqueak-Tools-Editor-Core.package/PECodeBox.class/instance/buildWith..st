building
buildWith: aBuilder

	self collapsed: false.	
	^ aBuilder build: (PEPluggablePanelSpec new
		layout: #vertical;
		horizontalResizing: #rigid;
		verticalResizing: #shrinkWrap;
		color: #color;
		model: self;
		padding: 3;
		spacing: 3;
		children: #buildChildren;
		yourself)