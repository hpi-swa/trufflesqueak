toolbuilder
buildCodePaneWith: builder
	^ (super buildCodePaneWith: builder)
		help: #helpText;
		stylerClass: PolyglotTextStyler;
		yourself