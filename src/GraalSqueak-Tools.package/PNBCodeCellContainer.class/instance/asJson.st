serialization
asJson

	| json  kernelspec languageInfo metadata cellsJson |
	json := JsonObject new.

	cellsJson := self submorphs collect: [:cell | cell asJson].
	
	languageInfo := JsonObject new
			at: 'codemirror_mode' put: (JsonObject new
				at: 'name' put: 'ipython';
				at: 'version' put: 3;
				yourself);
			at: 'file_extension' put: '.py';
			at: 'mimetype' put: 'text/x-python';
			at: 'name' put: 'python';
			at: 'nbconvert_exporter' put: 'python';
			at: 'pygments_lexer' put: 'ipython3';
			at: 'version' put: '3.5.2';
			yourself.
	
	kernelspec := JsonObject new
			at: 'display_name' put: 'Python 3';
			at: 'language' put: 'python';
			at: 'name' put: 'python3';
			yourself.
	
	metadata := JsonObject new
		at: 'kernelspec' put: kernelspec;
		at: 'language_info' put: languageInfo;
		yourself.
	
	json at: 'cells' put: cellsJson.
	json
		at: 'metadata' put: metadata;
		at: 'nbformat' put: 4;
		at: 'nbformat_minor' put: 2.
	
	^ json