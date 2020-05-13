accessing
collapseCodeBox

	"| snippets snippetFile |"
	self collapsed: self collapsed not.
	"snippets := self fileEntry parent children detect: [:each | each name = 'snippets'].
	snippetFile := (snippets children detect: [:each | each name = self fileName])."
	"self code: snippetFile read"
	"self code: (self addCodeBoxes: self fileEntry read)"
	