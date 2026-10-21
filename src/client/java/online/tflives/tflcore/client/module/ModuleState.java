package online.tflives.tflcore.client.module;

/** Lifecycle state of a module. FAILED modules stay disabled until the user re-enables them. */
public enum ModuleState {
    REGISTERED,
    ENABLED,
    DISABLED,
    MISSING_DEPENDENCY,
    FAILED
}
