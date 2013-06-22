package com.khmelyuk.multirun;

import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessListener;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.util.Key;

/**
 * Custom descriptor that allows/disallows its reusing.
 *
 * @author Ruslan Khmelyuk
 */
public class MyRunContentDescriptor extends RunContentDescriptor {

    public MyRunContentDescriptor(final RunContentDescriptor descriptor) {
        super(descriptor.getExecutionConsole(), descriptor.getProcessHandler(),
              descriptor.getComponent(), descriptor.getDisplayName(), descriptor.getIcon());
        if (descriptor.getProcessHandler() != null) {
            //noinspection ConstantConditions
            descriptor.getProcessHandler().addProcessListener(new ProcessListener() {
                @Override
                public void startNotified(ProcessEvent processEvent) {
                    if (MyRunContentDescriptor.this.getAttachedContent() != null) {
                        MyRunContentDescriptor.this.getAttachedContent().setPinned(true);
                    }
                }

                @Override
                public void processTerminated(ProcessEvent processEvent) { }

                @Override
                public void processWillTerminate(ProcessEvent processEvent, boolean b) { }

                @Override
                public void onTextAvailable(ProcessEvent processEvent, Key key) { }

            });
        }
    }
}
