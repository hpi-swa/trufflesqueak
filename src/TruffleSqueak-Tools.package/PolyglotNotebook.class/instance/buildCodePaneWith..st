toolbuilder
buildCodePaneWith: builder
	| textSpec |
	textSpec := builder pluggableScrollPaneSpec new.
	textSpec
		layout: #vertical;
		borderWidth: 0;
		morph: self cellContainer.
	^textSpec