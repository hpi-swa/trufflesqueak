building
buildBodyWith: aBuilder

	^ aBuilder pluggablePanelSpec new
		layout: #vertical;
		verticalResizing: #shrinkWrap;
		children: {self buildImportRowWith: aBuilder.
			self buildCodeAreaWith: aBuilder.
			self buildExportRowWith: aBuilder};
		yourself