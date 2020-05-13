toolbuilder
buildWith: builder
	| windowSpec window |
	windowSpec := self buildWindowWith: builder specs: {
		(0@0corner: 0.6@1) -> [self buildCodePaneWith: builder].
		(0.6@0corner: 1@1) -> [self buildSidebar: builder].
	}.
	windowSpec
		label: 'Polyglot Notebook';
		extent: 1000@720.
	window := builder build: windowSpec.
	(window allMorphs select: [:m | m isKindOf: PluggableButtonMorphPlus ]) do: [:b | b borderWidth: 0].
	(window allMorphs detect: [:m |  m isKindOf: PluggableTreeMorph ]) borderWidth: 0; color: (Color fromString: '#F5F5F5').
	self changed: #expandRootsRequested.
	^ window.