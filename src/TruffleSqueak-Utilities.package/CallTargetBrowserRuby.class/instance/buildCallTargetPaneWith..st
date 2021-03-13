toolbuilder
buildCallTargetPaneWith: builder
	| textSpec |
	textSpec := builder pluggableTextSpec new.
	textSpec 
		model: self;
		getText: #callTargetInfo.
	^ textSpec