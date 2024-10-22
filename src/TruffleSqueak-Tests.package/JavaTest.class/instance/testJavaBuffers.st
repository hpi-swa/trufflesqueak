testing
testJavaBuffers
	| buffer byteArray |
	buffer := (Java type: 'java.nio.ByteBuffer') allocate: 32.
	self assert: (Interop hasBufferElements: buffer).
	self assert: 32 equals: (Interop getBufferSize: buffer).
	self assert: (Interop isBufferWritable: buffer).
	
	self assert: 42 equals: (Interop writeBufferByte: buffer index: 0 value: 42).
	self assert: 42 equals: (Interop readBufferByte: buffer index: 0).
	self assert: 512 equals: (Interop writeBufferShort: buffer index: 1 value: 512).
	self assert: 512 equals: (Interop readBufferShort: buffer index: 1).
	self assert: 20000 equals: (Interop writeBufferInt: buffer index: 2 value: 20000).
	self assert: 20000 equals: (Interop readBufferInt: buffer index: 2).
	self assert: 1 << 16 equals: (Interop writeBufferLong: buffer index: 3 value: 1 << 16).
	self assert: 1 << 16 equals: (Interop readBufferInt: buffer index: 3).
	self assert: 2.1 equals: (Interop writeBufferFloat: buffer index: 4 value: 2.1).
	self assert: 2.0999999046325684 equals: (Interop readBufferFloat: buffer index: 4).
	self assert: 4.32198 equals: (Interop writeBufferDouble: buffer index: 5 value: 4.32198).
	self assert: 4.32198 equals: (Interop readBufferDouble: buffer index: 5).

	byteArray := ByteArray new: 17.
	Interop readBuffer: buffer byteOffset: 0 destination: byteArray destinationOffset: 1 length: byteArray size - 1.
	self assert: #[0 42 0 32 0 102 68 221 7 32 181 73 17 64 0 0 0] equals: byteArray