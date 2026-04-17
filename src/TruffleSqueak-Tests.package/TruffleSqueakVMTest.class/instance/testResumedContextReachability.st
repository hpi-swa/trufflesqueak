testing
testResumedContextReachability
    | ready resume finished results |
    ready := Semaphore new.
    resume := Semaphore new.
    finished := Semaphore new.
    
    [
        [
            | uniqueObject instances |
            "Create object locally, only reachable from this Context."
            uniqueObject := MessageSend receiver: #TruffleSqueak selector: #reachabilityTest.
            
            "Force this Context to be run by ResumeContextRootNode when it resumes."
            ready signal.
            resume wait.
                        
            "Force GC while resumed."
            Smalltalk garbageCollect.
            
            "Gather the instances. If the Context isn't linked, uniqueObject will not be found among the instances."
            instances := MessageSend allInstances select: [ :each | 
                each selector == #reachabilityTest 
            ].
            
            "Send result to the main thread."
            results := { uniqueObject. instances }.
            
        ] ensure: [ 
            "Guarantee the main thread is woken up, even if an exception occurs"
            finished signal 
        ].
    ] fork.
    
    "Wait for the fork to initialize and suspend"
    ready wait.
    
    "Resume the fork, triggering ResumeContextRootNode"
    resume signal.
    
    "Wait for the fork to complete GC and data gathering"
    finished wait.
    
    self assert: results notNil description: 'Forked process failed to gather results'.
    self assert: results second size = 1.
    self assert: results second first == results first.
