code-generation
fileName

	"If this is a new code box, generate a file name based on the box name. If it is an old code box, use the existing file name"
	^ self fileEntry ifNotNil: [self fileEntry name] ifNil: ['{1}.{2}' format: {
		(FileDirectory checkName: self boxName fixErrors: true).
		self innerLanguageInfo defaultExtension}]