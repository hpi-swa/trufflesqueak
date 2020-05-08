testing
testJavaInetAddress
	"Issue #78"
	self assert: '192.168.0.1' equals: ((Java type: 'java.net.InetAddress') getByAddress: #[192 168 0 1]) getHostAddress