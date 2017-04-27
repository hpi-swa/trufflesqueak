package de.hpi.swa.trufflesqueak.exceptions;

public class NonLocalReturn extends Return {
    private static final long serialVersionUID = 1L;
    private final Object target;

    public NonLocalReturn(Object top, Object closure) {
        super(top);
        target = closure;
    }

    public Object getTarget() {
        return target;
    }
}
