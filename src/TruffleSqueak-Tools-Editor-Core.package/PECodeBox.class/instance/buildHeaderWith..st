building
buildHeaderWith: aBuilder

	^ aBuilder pluggablePanelSpec new
		layout: #horizontal;
		"verticalResizing: #shrinkWrap;"
		horizontalResizing: #spaceFill;
		minimumHeight: TextStyle defaultFont height + 2;
		children: ((self buildNameRowWith: aBuilder), 
			(self buildLanguageButtonWith: aBuilder),
			(self buildCollapseButtonWith: aBuilder));
		yourself