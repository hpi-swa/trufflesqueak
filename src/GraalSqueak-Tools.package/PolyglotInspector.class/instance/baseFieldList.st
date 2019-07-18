overrides
baseFieldList
	"Answer an Array consisting of 'self'
	and the instance variable names of the inspected object."

	^ (Array with: 'self' with: 'all inst vars')
			, object allInstVarNamesEverywhere