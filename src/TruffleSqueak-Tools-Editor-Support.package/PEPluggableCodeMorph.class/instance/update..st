updating
update: aSymbol
	"overwrite super to check for a custom setEditTextSelector instead of simply listening for #editText"
	aSymbol ifNil: [^ self].
	aSymbol == self setEditTextSelector ifTrue: [
		self editString: (self model perform: self setEditTextSelector).
		^ self hasUnacceptedEdits: true].
	aSymbol == self setLineEndingsSelector ifTrue: [
		^ self textMorph editor lineEndings: (self model perform: self setLineEndingsSelector)].
	
	^ super update: aSymbol