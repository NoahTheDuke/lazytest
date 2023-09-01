package lazytest;

import clojure.lang.IExceptionInfo;
import clojure.lang.IPersistentMap;

public class ExpectationFailed extends Throwable implements IExceptionInfo {
    public final IPersistentMap reason;

    public ExpectationFailed(IPersistentMap reason) {
        super("Expectation Failed");

        if (reason == null) {
            throw new IllegalArgumentException("Additional data must be non-nil.");
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
