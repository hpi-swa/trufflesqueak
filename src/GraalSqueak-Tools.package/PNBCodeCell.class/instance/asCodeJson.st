serialization
asCodeJson

	^ JsonObject new
		at: 'cell_type' put: 'code';
		at: 'metadata' put: (JsonObject new
			at: 'polyjus_language' put: self languageID asString;
			yourself);
		at: 'execution_count' put: (executionCount ifNil: [0]);
		at: 'outputs' put: self outputsAsJson;
		at: 'source' put: {self editor text};
		yourself