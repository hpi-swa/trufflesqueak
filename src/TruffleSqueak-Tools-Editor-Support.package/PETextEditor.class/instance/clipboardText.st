private
clipboardText

	"Ensure correct line endings when pasting from clipboard"
	^ PELineEndingManager with: self lineEndings string: Clipboard clipboardText