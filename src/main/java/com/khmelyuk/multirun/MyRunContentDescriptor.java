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

    boolean reusable = false;

    public MyRunContentDescriptor(final RunContentDescriptor descriptor, boolean reusable) {
        super(descriptor.getExecutionConsole(), descriptor.getProcessHandler(),
              descriptor.getComponent(), descriptor.getDisplayName(), descriptor.getIcon());
        descriptor.getProcessHandler().addProcessListener(new ProcessListener() {
            @Override
            public void startNotified(ProcessEvent processEvent) {
            }

            @Override
            public void processTerminated(ProcessEvent processEvent) {
                MyRunContentDescriptor.this.reusable = true;
            }

            @Override
            public void processWillTerminate(ProcessEvent processEvent, boolean b) {
                MyRunContentDescriptor.this.reusable = true;
            }


            @Override
            public void onTextAvailable(ProcessEvent processEvent, Key key) {
            }
        }

        );
//        descriptor.getAttachedContent().setPinned(true);
        this.reusable = reusable;
    }

    @Override
    public boolean isContentReuseProhibited() {
        return !reusable;
    }
}
