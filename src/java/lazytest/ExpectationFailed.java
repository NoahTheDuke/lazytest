package lazytest;

import clojure.lang.IExceptionInfo;
import clojure.lang.IPersistentMap;
import clojure.lang.PersistentHashMap;

public class ExpectationFailed extends AssertionError implements IExceptionInfo {
    public final IPersistentMap reason;

    private static final long serialVersionUID = 1L;
    private static final String defaultMessage = "Expectation failed";

    public ExpectationFailed(IPersistentMap reason) {
        this(defaultMessage, reason);
    }

    public ExpectationFailed(String message, IPersistentMap reason) {
        super(message == null ? defaultMessage : message);

        if (reason == null) {
            reason = PersistentHashMap.EMPTY;
        }
        this.reason = reason;
    }

    public IPersistentMap getData() {
        return reason;
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }

    public String toString() {
        return "lazytest.ExpectationFailed: " + reason.toString();
    }
}
