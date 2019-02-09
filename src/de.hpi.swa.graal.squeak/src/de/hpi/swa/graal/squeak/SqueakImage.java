package de.hpi.swa.graal.squeak;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.interop.CanResolve;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.MessageResolution;
import com.oracle.truffle.api.interop.Resolve;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.graal.squeak.image.SqueakImageContext;

public final class SqueakImage implements TruffleObject {
    private final SqueakImageContext image;

    public SqueakImage(final SqueakImageContext image) {
        this.image = image;
    }

    public String getName() {
        return image.getImagePath();
    }

    @Override
    public ForeignAccess getForeignAccess() {
        return SqueakImageMessageResolutionForeign.ACCESS;
    }

    protected static boolean isInstance(final TruffleObject object) {
        return object instanceof SqueakImage;
    }

    @MessageResolution(receiverType = SqueakImage.class)
    public abstract static class SqueakImageMessageResolution {

        @Resolve(message = "READ")
        public abstract static class ReadNode extends Node {
            Object access(final SqueakImage squeakImage, final String name) {
                if ("Compiler".equals(name)) {
                    return squeakImage.image.getCompilerClass();
                } else {
                    // TODO:
                    return squeakImage.image.getGlobals();
                }
            }
        }

        @Resolve(message = "IS_EXECUTABLE")
        public abstract static class IsExecutableNode extends Node {
            boolean access(@SuppressWarnings("unused") final SqueakImage squeakImage) {
                return true;
            }
        }

        @Resolve(message = "EXECUTE")
        public abstract static class ExecuteNode extends Node {
            Object access(final SqueakImage squeakImage, final Object[] args) {
                assert args.length == 0;
                squeakImage.image.interrupt.start();
                squeakImage.image.disableHeadless();
                return Truffle.getRuntime().createCallTarget(squeakImage.image.getActiveContextNode()).call();
            }
        }

        @CanResolve
        public abstract static class CanResolveSqueakImage extends Node {
            boolean test(final TruffleObject object) {
                return object instanceof SqueakImage;
            }
        }
    }
}
