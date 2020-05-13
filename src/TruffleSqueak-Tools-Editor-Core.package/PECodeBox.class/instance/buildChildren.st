accessing
buildChildren

	| builder |
	builder := ToolBuilder default.
	^ self collapsed
	ifTrue: [{(self buildHeaderWith: builder) buildWith: builder}]
	ifFalse: [{(self buildBodyWith: builder) buildWith: builder.
		(self buildHeaderWith: builder) buildWith: builder}]