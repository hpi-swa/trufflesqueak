toolbuilder
buildCodePaneWith: builder
	^ (super buildCodePaneWith: builder)
		help: #helpText;
		yourself