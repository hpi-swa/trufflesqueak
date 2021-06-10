toolbuilder
buildCodePaneWith: builder
	^ (super buildCodePaneWith: builder)
		stylerClass: PolyglotTextStyler;
		help: #helpText;
		yourself