package com.khmelyuk.multirun;

import com.intellij.execution.ui.RunContentDescriptor;

/**
 * Custom descriptor that allows/disallows its reusing.
 *
 * @author Ruslan Khmelyuk
 */
public class MyRunContentDescriptor extends RunContentDescriptor {

    boolean reusable = false;

    public MyRunContentDescriptor(RunContentDescriptor descriptor, boolean reusable) {
        super(descriptor.getExecutionConsole(), descriptor.getProcessHandler(),
              descriptor.getComponent(), descriptor.getDisplayName(), descriptor.getIcon());
        this.reusable = reusable;
    }

    @Override
    public boolean isContentReuseProhibited() {
        return !reusable;
    }
}
