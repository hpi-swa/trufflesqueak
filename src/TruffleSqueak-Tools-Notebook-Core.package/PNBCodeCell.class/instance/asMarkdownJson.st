serialization
asMarkdownJson

	^ JsonObject new
		at: 'cell_type' put: 'markdown';
		at: 'metadata' put: JsonObject new;
		at: 'source' put: {self editor text};
		yourself