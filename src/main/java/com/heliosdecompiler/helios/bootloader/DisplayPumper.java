/*
 * Copyright 2016 Sam Sun <me@samczsun.com>
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.heliosdecompiler.helios.bootloader;

import com.heliosdecompiler.helios.Resources;
import com.heliosdecompiler.helios.handler.ExceptionHandler;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import java.util.concurrent.atomic.AtomicBoolean;

public class DisplayPumper implements Runnable {
    private final AtomicBoolean ready = new AtomicBoolean(false);
    private final Object synchronizer = new Object();

    private Display display;
    private Shell shell;

    public void run() {
        display = Display.getDefault();
        shell = new Shell(display);
        Resources.loadAllImages();
        ready.set(true);
        int i = 0;
        while (!display.isDisposed() && !shell.isDisposed()) {
            try {
                while (display.readAndDispatch()) ;
            } catch (Throwable e) {
                ExceptionHandler.handle(e);
            }
            display.sleep();
        }
        synchronized (synchronizer) {
            synchronizer.notifyAll();
        }
    }

    public Display getDisplay() {
        return this.display;
    }

    public Shell getShell() {
        return shell;
    }

    public boolean isReady() {
        return ready.get();
    }

    public Object getSynchronizer() {
        return this.synchronizer;
    }
}
