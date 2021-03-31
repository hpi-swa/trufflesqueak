testing
testBuffers
	"Buffers in Smalltalk are not (yet) supported"
	self deny: (Interop hasBufferElements: Smalltalk).