toolbuilder
buildCodePaneWith: builder
	^ (super buildCodePaneWith: builder)
		stylerClass: PolyglotTextStyler;
		yourself