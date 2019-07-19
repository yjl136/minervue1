package com.ramy.minervue.util;

import android.os.AsyncTask;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Created by peter on 2/19/14.
 */
public class ATask<Params, Progress, Result> extends AsyncTask<Params, Progress, Result> {

    private static final Executor EXECUTOR = Executors.newCachedThreadPool();

    /**
     * Cancel the task and wait for it to finish. Subclasses using this functionality MUST call
     * the super {@link #doInBackground(Object[])}.
     * @param mayInterrupt May interrupt.
     * @return <tt>true</tt> if successfully cancelled.
     */
    public synchronized boolean cancelAndClear(boolean mayInterrupt) {
        if (cancel(mayInterrupt)) {
            try {
                wait();
            } catch (InterruptedException e) {
                // Ignored.
            }
            return true;
        }
        return false;
    }

    /**
     * Wait for the task to finish. This task can not be one that is cancelled.
     */
    public void clear() {
        try {
            get();
        } catch (Exception e) {
            // Ignored.
        }
    }

    @Override
    protected synchronized Result doInBackground(Params... params) {
        notifyAll();
        return null;
    }

    public boolean isFinished() {
        return getStatus() == Status.FINISHED;
    }

    public void start(Params... params) {
        executeOnExecutor(EXECUTOR, params);
    }

}
