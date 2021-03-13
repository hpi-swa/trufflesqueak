toolbuilder
buildWith: builder
	^ builder build: (self buildWindowWith: builder specs: {
		(0@0 corner: 0.25@1) -> [self buildModuleListWith: builder].
		(0.25@0 corner: 0.5@1) -> [self buildMethodListWith: builder].
		(0.5@0 corner: 1@1) -> [self buildCallTargetPaneWith: builder].
	})